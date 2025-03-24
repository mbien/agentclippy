package dev.mbien.agentclippy;

import java.io.IOException;
import java.lang.classfile.ClassFile;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.Label;
import java.lang.constant.ClassDesc;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.constant.DynamicCallSiteDesc;
import java.lang.constant.MethodHandleDesc;
import java.lang.constant.MethodTypeDesc;
import java.nio.file.Path;
import java.util.function.Consumer;

import static java.lang.classfile.ClassFile.ACC_PRIVATE;
import static java.lang.constant.ConstantDescs.CD_Object;
import static java.lang.constant.ConstantDescs.MTD_void;
import static java.lang.constant.ConstantDescs.CD_void;
import static java.lang.constant.ConstantDescs.INIT_NAME;

/**
 *
 * @author mbien
 */
public class AgentClippy {
    
    private static final ClassDesc CD_WClipboard = ClassDesc.of("sun.awt.windows.WClipboard");
    private static final ClassDesc CD_AppContext = ClassDesc.of("sun.awt.AppContext");
    private static final ClassDesc CD_SunToolkit = ClassDesc.of("sun.awt.SunToolkit");
    private static final ClassDesc CD_SunClipBoard = ClassDesc.of("sun.awt.datatransfer.SunClipboard");
    
    /// Generate bytecode exactly matching the javac output of the patched WClipboard classfile.
    /// The two methods and their javap -v -p outputs are in the comments for reference.
    public static void main(String[] args) throws IOException, InterruptedException {
        
        // intercept / transform
        
        ClassFile.of().buildTo(Path.of("WClipboard.class"), CD_WClipboard, clb -> {
            clb.withFlags(ClassFile.ACC_PUBLIC)
                .withMethodBody(INIT_NAME, MTD_void, ClassFile.ACC_PUBLIC, cob -> {
                    cob.aload(0)
                       .invokespecial(CD_Object, INIT_NAME, MTD_void)
                       .return_();
                })
                .withMethodBody("handleContentsChanged", MTD_void, ACC_PRIVATE, generate_HandleContentsChanged)
                .withMethodBody("handleContentsChanged0", MTD_void, ACC_PRIVATE, generate_ContentsChanged0);
        });
        // dump generated class file to console
        new ProcessBuilder("javap", "-v", "-p", "WClipboard.class").inheritIO().start().waitFor();
    }

    //    private void handleContentsChanged() {
    //        AppContext appContext = AppContext.getAppContext();
    //        if (appContext == null) {
    //            handleContentsChanged0();
    //            return;
    //        }
    //        SunToolkit.postEvent(appContext, new InvocationEvent(Toolkit.getDefaultToolkit(), this::handleContentsChanged0));
    //    }
    private static final Consumer<CodeBuilder> generate_HandleContentsChanged = cob -> {
        // https://github.com/openjdk/jdk/blob/df9210e6578acd53384ee1ac06601510c9a52696/test/jdk/jdk/classfile/LowAdaptTest.java#L59
        // https://github.com/openjdk/jdk/blob/6705a9255d28f351950e7fbca9d05e73942a4e27/src/java.base/share/classes/java/lang/invoke/LambdaMetafactory.java#L336
        DirectMethodHandleDesc bsm = MethodHandleDesc.ofMethod(
                DirectMethodHandleDesc.Kind.STATIC,
                ClassDesc.of("java.lang.invoke.LambdaMetafactory"), "metafactory",
                MethodTypeDesc.ofDescriptor(
                         "(Ljava/lang/invoke/MethodHandles$Lookup;"
                        + "Ljava/lang/String;"
                        + "Ljava/lang/invoke/MethodType;"
                        + "Ljava/lang/invoke/MethodType;"
                        + "Ljava/lang/invoke/MethodHandle;"
                        + "Ljava/lang/invoke/MethodType;)"
                        + "Ljava/lang/invoke/CallSite;"
                )
        );
        Label label1 = cob.newLabel();
        cob.invokestatic(CD_AppContext, "getAppContext", MethodTypeDesc.of(CD_AppContext))
           .astore(1)
           .aload(1)
           .ifnonnull(label1)
           .aload(0)
           .invokevirtual(CD_WClipboard, "handleContentsChanged0", MTD_void)
           .return_()
           .labelBinding(label1)
           .aload(1)
           .new_(ClassDesc.of("java.awt.event.InvocationEvent"))
           .dup()
           .invokestatic(CD_SunToolkit, "getDefaultToolkit", MethodTypeDesc.of(CD_SunToolkit))
           .aload(0)
           .invokedynamic(DynamicCallSiteDesc.of(bsm, "run", MethodTypeDesc.of(ClassDesc.of("java.lang.Runnable"), CD_WClipboard)))
           .invokespecial(ClassDesc.of("java.awt.event.InvocationEvent"), INIT_NAME, MethodTypeDesc.of(CD_void, ClassDesc.of("java.lang.Object"), ClassDesc.of("java.lang.Runnable")))
           .invokestatic(CD_SunToolkit, "postEvent", MethodTypeDesc.of(CD_void, CD_AppContext, ClassDesc.of("java.awt.AWTEvent")))
           .return_();
        // original javac output
        //         0: invokestatic  #89                 // Method sun/awt/AppContext.getAppContext:()Lsun/awt/AppContext;
        //         3: astore_1
        //         4: aload_1
        //         5: ifnonnull     13
        //         8: aload_0
        //         9: invokevirtual #95                 // Method handleContentsChanged0:()V
        //        12: return
        //        13: aload_1
        //        14: new           #98                 // class java/awt/event/InvocationEvent
        //        17: dup
        //        18: invokestatic  #100                // Method java/awt/Toolkit.getDefaultToolkit:()Ljava/awt/Toolkit;
        //        21: aload_0
        //        22: invokedynamic #106,  0            // InvokeDynamic #0:run:(Lsun/awt/windows/WClipboard;)Ljava/lang/Runnable;
        //        27: invokespecial #110                // Method java/awt/event/InvocationEvent."<init>":(Ljava/lang/Object;Ljava/lang/Runnable;)V
        //        30: invokestatic  #113                // Method sun/awt/SunToolkit.postEvent:(Lsun/awt/AppContext;Ljava/awt/AWTEvent;)V
        //        33: return
    };

    //    private void handleContentsChanged0() {
    //        long[] formats = null;
    //        synchronized (this) {
    //            try {
    //                openClipboard(null);
    //                formats = getClipboardFormats();
    //            } catch (IllegalStateException exc) {
    //                // do nothing to handle the exception, call checkChange(null)
    //            } finally {
    //                closeClipboard();
    //            }
    //        }
    //        checkChange(formats);
    //    }
    private static final Consumer<CodeBuilder> generate_ContentsChanged0 = cob -> {
        Label label6 = cob.newLabel();
        Label label16 = cob.newLabel();
        Label label23 = cob.newLabel();
        Label label31 = cob.newLabel();
        Label label33 = cob.newLabel();
        Label label42 = cob.newLabel();
        Label label45 = cob.newLabel();
        Label label49 = cob.newLabel();

        Label label40 = cob.newLabel();
        Label label52 = cob.newLabel();
        cob.aconst_null()
           .astore(1)
           .aload(0)
           .dup()
           .astore(2)
           .monitorenter()
           .labelBinding(label6)
           .aload(0)
           .aconst_null()
           .invokevirtual(CD_WClipboard, "openClipboard", MethodTypeDesc.of(CD_void, CD_SunClipBoard))
           .aload(0)
           .invokevirtual(CD_WClipboard, "getClipboardFormats", MethodTypeDesc.of(ClassDesc.ofDescriptor("[J")))
           .astore(1)
           .labelBinding(label16)
           .aload(0)
           .invokevirtual(CD_WClipboard, "closeClipboard", MTD_void)
           .goto_(label40)
           .labelBinding(label23)
           .astore(3)
           .aload(0)
           .invokevirtual(CD_WClipboard, "closeClipboard", MTD_void)
           .goto_(label40)
           .labelBinding(label31)
           .astore(4)
           .labelBinding(label33)
           .aload(0)
           .invokevirtual(CD_WClipboard, "closeClipboard", MTD_void)
           .aload(4)
           .athrow()
           .labelBinding(label40)
           .aload(2)
           .monitorexit()
           .labelBinding(label42)
           .goto_(label52)
           .labelBinding(label45)
           .astore(5)
           .aload(2)
           .monitorexit()
           .labelBinding(label49)
           .aload(5)
           .athrow()
           .labelBinding(label52)
           .aload(0)
           .aload(1)
           .invokevirtual(CD_WClipboard, "checkChange", MethodTypeDesc.of(CD_void, ClassDesc.ofDescriptor("[J")))
           .exceptionCatch(label6, label16, label23, ClassDesc.of("java.lang.IllegalStateException"))
           .exceptionCatchAll(label6, label16, label31)
           .exceptionCatchAll(label31, label33, label31)
           .exceptionCatchAll(label6, label42, label45)
           .exceptionCatchAll(label45, label49, label45)
           .return_();
        // original javac output
        //    descriptor: ()V
        //    flags: (0x0002) ACC_PRIVATE
        //    Code:
        //      stack=2, locals=6, args_size=1
        //         0: aconst_null
        //         1: astore_1
        //         2: aload_0
        //         3: dup
        //         4: astore_2
        //         5: monitorenter
        //         6: aload_0
        //         7: aconst_null
        //         8: invokevirtual #27                 // Method openClipboard:(Lsun/awt/datatransfer/SunClipboard;)V
        //        11: aload_0
        //        12: invokevirtual #119                // Method getClipboardFormats:()[J
        //        15: astore_1
        //        16: aload_0
        //        17: invokevirtual #75                 // Method closeClipboard:()V
        //        20: goto          40
        //        23: astore_3
        //        24: aload_0
        //        25: invokevirtual #75                 // Method closeClipboard:()V
        //        28: goto          40
        //        31: astore        4
        //        33: aload_0
        //        34: invokevirtual #75                 // Method closeClipboard:()V
        //        37: aload         4
        //        39: athrow
        //        40: aload_2
        //        41: monitorexit
        //        42: goto          52
        //        45: astore        5
        //        47: aload_2
        //        48: monitorexit
        //        49: aload         5
        //        51: athrow
        //        52: aload_0
        //        53: aload_1
        //        54: invokevirtual #125                // Method checkChange:([J)V
        //        57: return
        //      Exception table:
        //         from    to  target type
        //             6    16    23   Class java/lang/IllegalStateException
        //             6    16    31   any
        //            31    33    31   any
        //             6    42    45   any
        //            45    49    45   any
    };
}

