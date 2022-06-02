package it.unipi.dii.hadoop.performance;

import java.io.*;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONObject;

public class Test {
    private static int[] nReducerJob0 = {1};
    private static int[] nReducerJob1 = {1};
    private static int[] nReducerJob2 = {1, 2};
    private static int[] nLineSplits = {800000};
    private static double[] falsePositiveRates = {0.01, 0.05, 0.1};

    public static void setConfigFile(String path, int nReducerJob0, int nReducerJob1, int nReducerJob2,
                                     int nLineSplits, double p) {
        try {
            FileInputStream in = new FileInputStream(path);
            Properties prop = new Properties();
            prop.load(in);
            in.close();

            FileOutputStream out = new FileOutputStream(path);
            prop.setProperty("p", String.valueOf(p));
            prop.setProperty("job0-n-reducers", String.valueOf(nReducerJob0));
            prop.setProperty("job1-n-reducers", String.valueOf(nReducerJob1));
            prop.setProperty("job2-n-reducers", String.valueOf(nReducerJob2));
            prop.setProperty("verbose", String.valueOf(true));  // Needed to save counters info
            prop.store(out, null);
            out.close();
        }
        catch (IOException io) {
            io.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        JSONArray results = new JSONArray();
        String[] cmd = {"hadoop", "jar", "hadoop-1.0-SNAPSHOT.jar", "it.unipi.dii.hadoop.Driver"};

        for (int i0=0; i0<nReducerJob0.length; i0++) {
            for (int i1=0; i1<nReducerJob1.length; i1++) {
                for (int i2=0; i2<nReducerJob2.length; i2++) {
                    for (int j=0; j<falsePositiveRates.length; j++) {
                        System.out.println("> Iteration Configuration Parameters:");
                        System.out.println("\tnReducerJob0=" + nReducerJob0[i0]);
                        System.out.println("\tnReducerJob1=" + nReducerJob1[i1]);
                        System.out.println("\tnReducerJob2=" + nReducerJob2[i2]);
                        System.out.println("\tfalsePositiveRate=" + falsePositiveRates[j]);

                        // Setup the jobs configuration file
                        setConfigFile("config.properties", nReducerJob0[i0], nReducerJob1[i1],
                                        nReducerJob2[i2], 0, falsePositiveRates[j]);

                        // Run the hadoop command as a new process
                        ProcessBuilder builder = new ProcessBuilder(cmd);
                        Process process = builder.start();
                        process.waitFor();

                        // Convert hadoop process stdout to String
                        ByteArrayOutputStream into = new ByteArrayOutputStream();
                        byte[] buf = new byte[4096];
                        for (int n; 0 < (n = process.getInputStream().read(buf)); ) {
                            into.write(buf, 0, n);
                        }
                        into.close();
                        String output = into.toString("UTF-8");
                        String[] jobCounters = output.split(">");

                        // Add iteration results to JSON results array
                        JSONObject iterationResult = new JSONObject();
                        iterationResult.put("nReducerJob0", nReducerJob0[i0]);
                        iterationResult.put("nReducerJob1", nReducerJob1[i1]);
                        iterationResult.put("nReducerJob2", nReducerJob2[i2]);
                        iterationResult.put("falsePositiveRate", falsePositiveRates[j]);
                        iterationResult.put("countersJob0", jobCounters[1]);
                        iterationResult.put("countersJob1", jobCounters[2]);
                        iterationResult.put("countersJob2", jobCounters[3]);

                        results.put(iterationResult);

                        // Update file
                        try (PrintWriter out = new PrintWriter("hadoop_results.json")) {
                            out.println(results.toString(4));
                        } catch (Exception exception) {
                            exception.printStackTrace();
                            System.out.println(results.toString(4));
                        }
                    }
                }
            }
        }
    }
}