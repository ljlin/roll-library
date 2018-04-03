package roll;


import java.io.*;
import java.util.stream.Collectors;

public class NativeTool {
    static public String Dot2SVG(String dot) {
        ProcessBuilder builder = new ProcessBuilder(
                "/bin/bash",
                "-c","dot -Tsvg"
        );
        Process process = null;
        try {
            process = builder.start();
            OutputStream stdin = process.getOutputStream();
            InputStream stdout = process.getInputStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(stdout));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stdin));

            writer.write(dot);
            writer.flush();
            writer.close();

            String res = reader.lines().collect(Collectors.joining());

            reader.close();
            process.destroy();

            return res;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }
}
