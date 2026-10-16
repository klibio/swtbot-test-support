package io.klib.swtbot.fixture;

import java.util.Hashtable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Callable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public final class UiFixture implements BundleActivator {
    private static final CountDownLatch ready = new CountDownLatch(1);
    private static volatile Display display;

    @Override
    public void start(BundleContext context) {
        Hashtable<String, Object> properties = new Hashtable<>();
        properties.put("main.thread", true);
        context.registerService(Runnable.class, () -> {
            Display applicationDisplay = new Display();
            display = applicationDisplay;
            ready.countDown();
            try {
                while (!applicationDisplay.isDisposed()) {
                    if (!applicationDisplay.readAndDispatch()) {
                        applicationDisplay.sleep();
                    }
                }
            } finally {
                if (!applicationDisplay.isDisposed()) {
                    applicationDisplay.dispose();
                }
            }
        }, properties);
    }

    @Override
    public void stop(BundleContext context) {
        Display applicationDisplay = display;
        if (applicationDisplay != null && !applicationDisplay.isDisposed()) {
            applicationDisplay.asyncExec(applicationDisplay::dispose);
        }
    }

    public static Display display() throws InterruptedException {
        if (!ready.await(20, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Fixture Display not created on launch main thread");
        }
        return display;
    }

    public static <Result> Result ui(Callable<Result> action) throws Exception {
        Display applicationDisplay = display();
        if (Display.getCurrent() == applicationDisplay) {
            return action.call();
        }
        FutureTask<Result> task = new FutureTask<>(action);
        applicationDisplay.asyncExec(task);
        return task.get(10, TimeUnit.SECONDS);
    }

    public static Shell openShell() throws Exception {
        return ui(() -> {
            Shell shell = new Shell(display);
            shell.setText("SWT screenshot integration fixture");
            shell.setBackground(display.getSystemColor(SWT.COLOR_GREEN));
            shell.setSize(360, 220);
            shell.open();
            return shell;
        });
    }
}