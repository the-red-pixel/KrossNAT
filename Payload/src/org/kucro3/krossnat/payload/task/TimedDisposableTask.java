package org.kucro3.krossnat.payload.task;

import com.theredpixelteam.redtea.util.Predication;

public abstract class TimedDisposableTask extends DisposableTask {
    public TimedDisposableTask(long delayMillis)
    {
        this.timeStamp = System.currentTimeMillis();
        this.delayMillis = Predication.requirePositive(delayMillis);
    }

    @Override
    public void onRequeue()
    {
        this.timeStamp = System.currentTimeMillis();
    }

    @Override
    public boolean shouldExecute()
    {
        return (System.currentTimeMillis() - timeStamp) >= delayMillis;
    }

    public long getDelayMillis()
    {
        return delayMillis;
    }

    public void setDelayMillis(long delayMillis)
    {
        this.delayMillis = Predication.requirePositive(delayMillis);
    }

    @Override
    public abstract void run() throws Exception;

    public static TimedDisposableTask of(Runnable runnable, long delayMillis)
    {
        return new TimedDisposableTask(delayMillis) {
            @Override
            public void run() throws Exception
            {
                runnable.run();
            }
        };
    }

    private volatile long delayMillis;

    private volatile long timeStamp;
}
