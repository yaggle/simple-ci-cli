package co.yaggle.simpleci.cli;

import co.yaggle.simpleci.core.pipeline.Pipeline;
import co.yaggle.simpleci.core.pipeline.PipelineLoader;
import co.yaggle.simpleci.core.pipeline.PipelineRunner;
import co.yaggle.simpleci.core.pipeline.event.ContainerStartedEvent;
import co.yaggle.simpleci.core.pipeline.event.ContainerStoppedEvent;
import co.yaggle.simpleci.core.pipeline.event.ImageLoadFailedEvent;
import co.yaggle.simpleci.core.pipeline.event.ImageLoadedEvent;
import co.yaggle.simpleci.core.pipeline.event.PipelineCompletedEvent;
import co.yaggle.simpleci.core.pipeline.event.PipelineEvent;
import co.yaggle.simpleci.core.pipeline.event.PipelineStartedEvent;
import co.yaggle.simpleci.core.pipeline.event.TaskCommandAbortedEvent;
import co.yaggle.simpleci.core.pipeline.event.TaskCommandCompletedEvent;
import co.yaggle.simpleci.core.pipeline.event.TaskCommandOutputEvent;
import co.yaggle.simpleci.core.pipeline.event.TaskCommandStartedEvent;
import co.yaggle.simpleci.core.pipeline.event.TaskCompletedEvent;
import co.yaggle.simpleci.core.pipeline.event.TaskStartedEvent;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Main {

    /**
     * Very simplistic CLI pipeline runner. It runs the pipeline in the current working directory,
     * which is mounted to the Docker image.
     *
     * @param args the command line args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        File currentDirectory = new File(System.getProperty("user.dir"));

        System.out.println("Current directory: " + currentDirectory.getCanonicalPath());

        // Load the pipeline config from the current directory
        Pipeline pipeline = PipelineLoader.loadPipeline(currentDirectory);

        // Create an event queue to listen to events.
        BlockingQueue<PipelineEvent> eventQueue = new LinkedBlockingQueue<>();

        // Run the pipeline
        PipelineRunner
            .builder()
            .pipeline(pipeline)
            .mountFromDirectory(currentDirectory)
            .eventQueue(eventQueue)
            .build()
            .run(pipeline);


        PipelineEvent pipelineEvent = eventQueue.take();
        while (
            !(pipelineEvent instanceof PipelineCompletedEvent) &&
            !(pipelineEvent instanceof ImageLoadFailedEvent) &&
            !(pipelineEvent instanceof TaskCommandAbortedEvent)
        ) {
            if (pipelineEvent instanceof PipelineStartedEvent) {
                onPipelineStarted((PipelineStartedEvent) pipelineEvent);
            } else if (pipelineEvent instanceof ImageLoadedEvent) {
                onImageLoaded((ImageLoadedEvent) pipelineEvent);
            } else if (pipelineEvent instanceof ContainerStartedEvent) {
                onContainerStarted((ContainerStartedEvent) pipelineEvent);
            } else if (pipelineEvent instanceof TaskStartedEvent) {
                onTaskStarted((TaskStartedEvent) pipelineEvent);
            } else if (pipelineEvent instanceof TaskCommandStartedEvent) {
                onTaskCommandStarted((TaskCommandStartedEvent) pipelineEvent);
            } else if (pipelineEvent instanceof TaskCommandOutputEvent) {
                onTaskCommandOutput((TaskCommandOutputEvent) pipelineEvent);
            } else if (pipelineEvent instanceof TaskCommandCompletedEvent) {
                onTaskCommandCompleted((TaskCommandCompletedEvent) pipelineEvent);
            } else if (pipelineEvent instanceof TaskCompletedEvent) {
                onTaskCompleted((TaskCompletedEvent) pipelineEvent);
            } else if (pipelineEvent instanceof ContainerStoppedEvent) {
                onContainerStopped((ContainerStoppedEvent) pipelineEvent);
            }

            pipelineEvent = eventQueue.take();
        }

        if (pipelineEvent instanceof PipelineCompletedEvent) {
            System.out.println("PIPELINE COMPLETED!");
        } else if (pipelineEvent instanceof ImageLoadFailedEvent) {
            System.out.println("IMAGE LOAD FAILED!");
        } else if (pipelineEvent instanceof TaskCommandAbortedEvent) {
            System.out.println("TASK COMMAND ABORTED!");
        }
    }

    private static void onPipelineStarted(PipelineStartedEvent e) {
        System.out.println("PIPELINE STARTED!");
    }

    private static void onImageLoaded(ImageLoadedEvent e) {
        System.out.println("IMAGE LOADED!\n");
    }


    private static void onContainerStarted(ContainerStartedEvent e) {
        System.out.println("CONTAINER STARTED!\n");
    }


    private static void onTaskStarted(TaskStartedEvent e) {
        System.out.println("TASK STARTED: " + e.getTaskId() + "\n");
    }


    private static void onTaskCommandStarted(TaskCommandStartedEvent e) {
        System.out.println("TASK COMMAND STARTED: " + e.getTaskId() + " (" + e.getCommandIndex() + "): " + e.getCommand() + "\n");
    }


    private static void onTaskCommandOutput(TaskCommandOutputEvent e) {
        // This is a bit dodgy. Parallel tasks' output will be mixed together. Fix later :)
        System.out.print(e.getCharacters());
    }


    private static void onTaskCommandCompleted(TaskCommandCompletedEvent e) {
        System.out.println("TASK COMMAND COMPLETED: " + e.getTaskId() + " (" + e.getCommandIndex() + ")\n");
    }

    private static void onContainerStopped(ContainerStoppedEvent e) {
        System.out.println("CONTAINER STOPPED!");
    }

    private static void onTaskCompleted(TaskCompletedEvent e) {
        System.out.println("TASK COMPLETED: " + e.getTaskId() + "\n");
    }
}
