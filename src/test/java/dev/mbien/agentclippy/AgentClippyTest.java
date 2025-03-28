package dev.mbien.agentclippy;

import java.lang.classfile.ClassFile;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import sun.awt.windows.WClipboard;

import static java.lang.classfile.ClassFile.ACC_PRIVATE;
import static java.lang.classfile.ClassFile.ACC_PUBLIC;
import static java.lang.constant.ConstantDescs.CD_Object;
import static java.lang.constant.ConstantDescs.INIT_NAME;
import static java.lang.constant.ConstantDescs.MTD_void;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author mbien
 */
public class AgentClippyTest {
    
    @Test
    public void testGenerator() throws Exception {
        // Generate bytecode exactly matching the javac output of the patched WClipboard classfile.
        // The two methods and their javap -v -p outputs are in the comments for reference.
        ClassFile.of().buildTo(Path.of("WClipboard.class"), AgentClippy.CD_WClipboard, clb -> {
            clb.withFlags(ACC_PUBLIC)
                .withMethodBody(INIT_NAME, MTD_void, ACC_PUBLIC, cob -> {
                    cob.aload(0)
                       .invokespecial(CD_Object, INIT_NAME, MTD_void)
                       .return_();
                })
                .withMethodBody("handleContentsChanged", MTD_void, ACC_PRIVATE, AgentClippy.generate_HandleContentsChanged)
                .withMethodBody("handleContentsChanged0", MTD_void, ACC_PRIVATE, AgentClippy.generate_ContentsChanged0);
        });
        // dump generated class file to console
        System.out.println("- ".repeat(20));
        new ProcessBuilder("javap", "-v", "-p", "WClipboard.class").inheritIO().start().waitFor();
        System.out.println("- ".repeat(20));
    }
    
    @Test
    public void testTransformer() throws Exception {
        // force class loading and agent activation
        assertNotNull(WClipboard.class.getName());
        
        List<String> methods = List.of(WClipboard.class.getDeclaredMethods()).stream().map(Method::getName).toList();
        
        assertTrue(methods.contains("handleContentsChanged"));
        assertTrue(methods.contains("handleContentsChanged0"));
//        assertEquals(2, methods.size());
        
        System.out.println("- ".repeat(20));
        new ProcessBuilder("javap", "-v", "-p", "Transformed.class").inheritIO().start().waitFor();
        System.out.println("- ".repeat(20));
    }
    
}
