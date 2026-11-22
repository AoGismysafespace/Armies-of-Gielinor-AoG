import java.lang.reflect.Field;
import com.sun.tools.attach.VirtualMachine;

public class InspectLiveFps {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: InspectLiveFps <pid>");
            System.exit(1);
        }
        String pid = args[0];
        System.out.println("Attaching to PID " + pid + "...");
        VirtualMachine vm = VirtualMachine.attach(pid);
        System.out.println("Attached successfully!");
        // We can inspect properties or load agent
        String d3d = vm.getSystemProperties().getProperty("sun.java2d.d3d");
        System.out.println("sun.java2d.d3d: " + d3d);
        vm.detach();
    }
}
