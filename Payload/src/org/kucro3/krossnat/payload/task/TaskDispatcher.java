package org.kucro3.krossnat.payload.task;

public class TaskDispatcher {
    private TaskDispatcher()
    {
    }

    public static void dispath(TaskQueue taskQueue)
    {
        int size = taskQueue.count();

        for (int i = 0; i < size; i++)
        {
            Task task = taskQueue.pop();

            if (!task.shouldExecute())
            {
                taskQueue.queue(task);

                continue;
            }

            try {
                task.run();
            } catch (Exception e) {
                TaskExceptionHandler handler;

                if ((handler = task.getExceptionHandler()) != null)
                    handler.handle(e);
                else
                    throw new RuntimeException("unhandled task exception", e);
            }

            if (task.shouldRequeue())
                taskQueue.queue(task);
        }
    }
}
