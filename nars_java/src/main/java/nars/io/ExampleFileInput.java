/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package nars.io;

import nars.core.NAR;
import nars.io.condition.OutputCondition;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Access to library of examples/unit tests
 * TODO use getClass().getResource ?
 */
public class ExampleFileInput extends TextInput {

    public static String load(String path) throws IOException {
        StringBuilder  sb  = new StringBuilder();
        String line;
        File fp = new File(path);
        BufferedReader br = new BufferedReader(new FileReader(fp));
        while ((line = br.readLine())!=null) {
            sb.append(line).append("\n");
        }
        br.close();
        return sb.toString();
    }

    public static Iterable<String> getUnitTestPaths() {
        return getUnitTests().values();
        //Collection<String> v = getUnitTests().values();
        /*return Iterables.transform(v, new Function<String, String>() {

            @Override
            public String apply(String f) {
                return (String)((Object[])f)[0];
            }
            
        });*/
    }
    
    /** narsese source code, one instruction per line */
    private final String source;

    protected ExampleFileInput(TextPerception t, String input)  {
        super(t, input);
        this.source = input;
    }
    
    public static ExampleFileInput get(NAR n, String id) throws Exception {
        if (!id.endsWith(".nal"))
            id = id + ".nal";

        String path = getExamplePath(id);
                
        return new ExampleFileInput(n.textPerception, load(path));
    }

    final static String cwd;

    static {
        Path currentRelativePath = Paths.get("");
        cwd = currentRelativePath.toAbsolutePath().toString();
    }

    public static String getExamplePath(String path) {
        if (path.startsWith("/")) return path; //dont modify, it's already absolute
        if (cwd.endsWith("nars_java"))
            return "../nal/" + path;
        else
            return "nal/" + path;
    }
    
    public List<OutputCondition> enableConditions(NAR n, int similarResultsToSave) {
        return OutputCondition.getConditions(n, source, similarResultsToSave);
    }

    public static Map<String,String> getUnitTests() {
        return getUnitTests(new String[] { "test", "Examples/DecisionMaking", "Examples/ClassicalConditioning" });
    }

    public static Map<String,String> getUnitTests(String... directories) {
        Map<String,String> l = new TreeMap();

        for (String dir : directories ) {

            File folder = new File(getExamplePath(dir));
            File[] files = folder.listFiles();
            if (files == null) {
                System.err.println(folder.getAbsoluteFile() + " is not a directory or does not exist");
                break;
            }

            for (final File file : folder.listFiles()) {
                if (file.isDirectory() || file.getName().equals("README.txt") || file.getName().contains(".png"))
                    continue;
                if(!("extra".equals(file.getName()))) {
                    l.put(file.getName(), file.getAbsolutePath() );
                }
            }
            
        }
        return l;
    }

    public String getSource() {
        return source;
    }

    protected static Map<String, String> examples = new HashMap(); //path -> script data

    public static String getExample(String path) {
        try {
            String existing = examples.get(path);
            if (existing!=null)
                return existing;

            existing = ExampleFileInput.load(path);

            examples.put(path, existing);
            return existing;
        } catch (Exception ex) {
            throw new RuntimeException("Example file not found: " + path + ": " + ex.toString()  + ": ");
        }
    }
}
