package org.kucro3.krossnat.payload.task;

public abstract class DisposableTask extends AbstractTask {
    public DisposableTask()
    {
    }

    @Override
    public boolean shouldExecute()
    {
        return true;
    }

    @Override
    public boolean shouldRequeue()
    {
        return false;
    }

    @Override
    public abstract void run() throws Exception;

    public static DisposableTask of(Runnable runnable)
    {
        return new DisposableTask() {
            @Override
            public void run() throws Exception
            {
                runnable.run();
            }
        };
    }
}
