package sun.awt.windows;

/**
 * dummy target class for transformer
 */
public final class WClipboard /*extends SunClipboard*/ {

    public WClipboard() {
//        super("System");
        handleContentsChanged(); // avoid dead code elimination
    }

    /**
     * Upcall from native code.
     */
    private void handleContentsChanged() {
        System.out.println("no dead code");
//        if (!areFlavorListenersRegistered()) {
//            return;
//        }
//
//        long[] formats = null;
//        try {
//            openClipboard(null);
//            formats = getClipboardFormats();
//        } catch (IllegalStateException exc) {
//            // do nothing to handle the exception, call checkChange(null)
//        } finally {
//            closeClipboard();
//        }
//        checkChange(formats);
    }


}
