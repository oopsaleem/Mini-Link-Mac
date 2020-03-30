package com.kgwb;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.kgwb.model.MiniLinkMacWrapper;
import javafx.concurrent.Task;
import net.sf.expectit.Expect;
import net.sf.expectit.ExpectBuilder;
import net.sf.expectit.Result;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.sf.expectit.filter.Filters.removeColors;
import static net.sf.expectit.filter.Filters.removeNonPrintable;
import static net.sf.expectit.matcher.Matchers.regexp;
import static net.sf.expectit.matcher.Matchers.sequence;

public class LongRunningTask extends Task<List<MiniLinkMacWrapper>> {

    private final String filePath;
    private static final String CONS_PROMPT_PATTERN = ">";
    private static final String CONS_USER = "foo";
    private static final String CONS_PWD = "bar";
    private static final int CONS_PORT = 22;
    private static final String CONS_SHOW_MAC = "show mac-address-table";


    public LongRunningTask(String filePath) {
        this.filePath = filePath;
    }

    @Override
    protected List<MiniLinkMacWrapper> call() {
        ExecutorService executor = Executors.newCachedThreadPool();

        List<String> commands = new ArrayList<>();
        commands.add(CONS_SHOW_MAC);

        FileInputStream file;
        int itemCount = 0;
        try {
            file = new FileInputStream(new File(filePath));
            List<Callable<MiniLinkRowData>> listOfCallable = new ArrayList<>();
            try {
                XSSFWorkbook workbook = new XSSFWorkbook(file);
                XSSFSheet sheet = workbook.getSheetAt(0);
                Iterator<Row> rowIterator = sheet.iterator();

                rowIterator.next();
                while (rowIterator.hasNext()) {
                    Row row = rowIterator.next();
                    final String cell_NeName = row.getCell(0).getStringCellValue();
                    final String cell_IPv4 = row.getCell(1).getStringCellValue();

                    if (cell_IPv4 == null || cell_IPv4.trim().isEmpty()) continue;

                    itemCount++;

                    listOfCallable.add(() -> {
                        try {
                            JSch jSch = new JSch();
                            Session session = jSch.getSession(CONS_USER, cell_IPv4, CONS_PORT);
//                            session.setPassword(CONS_PWD);
                            Properties properties = new Properties();
                            properties.put("PreferredAuthentications", "publickey,keyboard-interactive,password");
                            properties.put("StrictHostKeyChecking", "no");
                            properties.put("UseDNS", "no");
                            session.setConfig(properties);
                            session.connect();
                            session.setTimeout(15 * 1_000);
                            Channel channel = session.openChannel("shell");
                            channel.connect();

                            try (Expect expect = new ExpectBuilder()
                                    .withTimeout(15, TimeUnit.SECONDS)
                                    .withOutput(channel.getOutputStream())
                                    .withInputs(channel.getInputStream(), channel.getExtInputStream())
//                                    .withEchoInput(System.out)
//                                    .withEchoOutput(System.err)
                                    .withInputFilters(removeColors(), removeNonPrintable())
                                    .withExceptionOnFailure()
                                    .build()) {
                                expect.expect(regexp("assword: "));
                                for (char c : CONS_PWD.toCharArray()) expect.send(String.valueOf(c));
                                expect.sendLine("");
                                Result welcome = expect.expect(sequence(regexp(".+"), regexp(CONS_PROMPT_PATTERN)));
                                // Grab configured node name
                                String neNameConfigured = "";
                                Pattern regexNeName = Pattern.compile("^(?<neName>WR-\\d+-[A-Z0-9]+)>", Pattern.MULTILINE);
                                Matcher matcherNeName = regexNeName.matcher(welcome.getInput());
                                if (matcherNeName.find()) {
                                    neNameConfigured = matcherNeName.group("neName");
                                }

                                // Execute commands
                                Map<String, String> cmds_result = new HashMap<>();
                                for (String cmd : commands) {
                                    expect.sendLine(cmd);
                                    Result commandResult = expect.expect(sequence(regexp(".+"), regexp(CONS_PROMPT_PATTERN)));
                                    cmds_result.put(cmd, commandResult.getInput());
                                }

                                return new MiniLinkRowData(
                                        neNameConfigured.contentEquals(cell_NeName) ? neNameConfigured : String.format("%s should be %s", cell_NeName, neNameConfigured)
                                        , cell_IPv4, cmds_result);
                            } finally {
                                channel.disconnect();
                                session.disconnect();
                            }
                        } catch (Exception e) {
                            Map<String, String> cmd_result = new HashMap<>();
                            cmd_result.put("EXCEPTION", "" + e.getMessage());
                            return new MiniLinkRowData(cell_NeName, cell_IPv4, cmd_result);
                        }
                    });
                }
            } catch (IOException e) {
                updateMessage(e.getMessage());
            } finally {
                try {
                    file.close();
                } catch (IOException e) {
                    updateMessage(e.getMessage());
                }
            }

            final int max = itemCount;

            List<Future<MiniLinkRowData>> futures = executor.invokeAll(listOfCallable);
            List<MiniLinkMacWrapper> listOfmlDevWrapper = new ArrayList<>();
            AtomicInteger iterations = new AtomicInteger();
            futures.stream().map(f -> {
                try {
                    return f.get(10, TimeUnit.SECONDS);
                } catch (Exception e) {
                    return e.getMessage();
                }
            }).forEach(rawItem -> {
                iterations.getAndIncrement();
                MiniLinkRowData deviceRAW = (MiniLinkRowData) rawItem;
                updateProgress(iterations.get() + 1, max);

                try {
                    Map<String, String> rawData = deviceRAW.getRawData(); // Map of command and its result to assess

                    if (rawData.containsKey("EXCEPTION")) { // Assess exception
                        String rawValue = rawData.get("EXCEPTION");
                        listOfmlDevWrapper.add(new MiniLinkMacWrapper(deviceRAW.getName(), deviceRAW.getIp(), rawValue));
                    } else { // Assess command printout
                        if (rawData.containsKey(CONS_SHOW_MAC)) {
                            String rawValue = rawData.get(CONS_SHOW_MAC);
                            if (rawValue.startsWith("EXCEPTION:"))
                                listOfmlDevWrapper.add(new MiniLinkMacWrapper(deviceRAW.getName(), deviceRAW.getIp(), rawValue));
                            else {
                                String value = rawData.get(CONS_SHOW_MAC);
                                if (assessMac(value))
                                    listOfmlDevWrapper.add(new MiniLinkMacWrapper(deviceRAW.getName(), deviceRAW.getIp(), "MAC AVAILABLE"));
                                else
                                    listOfmlDevWrapper.add(new MiniLinkMacWrapper(deviceRAW.getName(), deviceRAW.getIp(), "NO MAC"));
                            }
                        } else
                            listOfmlDevWrapper.add(new MiniLinkMacWrapper(deviceRAW.getName(), deviceRAW.getIp(), "No result for command: " + CONS_SHOW_MAC));
                    }
                } catch (Exception e) {
                    listOfmlDevWrapper.add(new MiniLinkMacWrapper(deviceRAW.getName(), deviceRAW.getIp(), e.getMessage()));
                }


            });

            return listOfmlDevWrapper;
        } catch (FileNotFoundException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }

        return null;
    }

    private boolean assessMac(String macPrint) {
        Pattern patter = Pattern.compile("([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})", Pattern.MULTILINE);
        Matcher matcher = patter.matcher(macPrint);
        return matcher.find();
    }
}

class MiniLinkRowData {
    String ip;
    String name;
    Map<String, String> rawData;

    public MiniLinkRowData(String name, String iPv4, Map<String, String> rawData) {
        this.name = name;
        this.ip = iPv4;
        this.rawData = rawData;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, String> getRawData() {
        return rawData;
    }

    public void setRawData(Map<String, String> rawData) {
        this.rawData = rawData;
    }
}
