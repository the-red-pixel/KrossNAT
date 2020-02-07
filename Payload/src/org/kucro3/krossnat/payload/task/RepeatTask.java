package org.kucro3.krossnat.payload.task;

public abstract class RepeatTask extends AbstractTask {
    public RepeatTask()
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
        return true;
    }

    @Override
    public abstract void run() throws Exception;

    public static RepeatTask of(Runnable runnable)
    {
        return new RepeatTask() {
            @Override
            public void run() throws Exception
            {
                runnable.run();
            }
        };
    }
}
