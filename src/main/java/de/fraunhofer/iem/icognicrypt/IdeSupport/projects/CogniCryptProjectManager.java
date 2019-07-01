package de.fraunhofer.iem.icognicrypt.IdeSupport.projects;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.wm.*;
import com.intellij.ui.content.Content;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import de.fraunhofer.iem.icognicrypt.core.BackgroundPackage;
import de.fraunhofer.iem.icognicrypt.core.Collections.IReadOnlyCollection;
import de.fraunhofer.iem.icognicrypt.core.Collections.ReadOnlyCollection;
import de.fraunhofer.iem.icognicrypt.results.CogniCryptErrorWindow;
import de.fraunhofer.iem.icognicrypt.ui.CogniCryptToolWindowFactory;
import de.fraunhofer.iem.icognicrypt.ui.CogniCryptWindowBase;
import org.jetbrains.annotations.NotNull;
import org.omg.CORBA.OBJ_ADAPTER;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import java.awt.*;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public final class CogniCryptProjectManager implements ProjectManagerListener
{
    private static CogniCryptProjectManager _instance;

    private final HashSet<Project> _projects = new HashSet<>();
    private Collection<WeakReference<ICogniCryptProjectListener>> _subscribers = new HashSet<>();

    private CogniCryptProjectManager()
    {
        _instance = this;
        System.out.println("CogniCryptProjectManager Thread: " + Thread.currentThread().getId());

        CogniCryptProjectManager.GetInstance();
        MessageBus bus = ApplicationManager.getApplication().getMessageBus();
        MessageBusConnection connection = bus.connect();
        connection.subscribe(ProjectManager.TOPIC, this);
    }

    public static CogniCryptProjectManager GetInstance()
    {
        if (_instance == null) _instance = new CogniCryptProjectManager();
        return _instance;
    }

    @Override
    public void projectOpened(@NotNull Project project)
    {
        if (_subscribers.isEmpty()) return;

        _subscribers.forEach(s ->
        {
            ICogniCryptProjectListener value = s.get();
            if (value != null) value.OnProjectOpened(project);
        });


        Task t = new Task.Backgroundable(project, "Wait until project is initialized", false)
        {
            @Override
            public void run(@NotNull ProgressIndicator indicator)
            {
                while (true)
                {
                    try
                    {
                        if (Thread.currentThread().isInterrupted())
                        {
                            System.out.println("Interrupted");
                            break;
                        }
                        if (project.isInitialized())
                        {
                            System.out.println("initialized");
                            break;
                        }
                        Thread.currentThread().yield();
                        Thread.currentThread().sleep(200);
                    }
                    catch (Exception e)
                    {
                        break;
                    }
                }
            }

            @Override
            public void onFinished()
            {
                super.onFinished();
                _subscribers.forEach(s ->
                {
                    ICogniCryptProjectListener value = s.get();
                    if (value != null) value.OnProjectInitialized(project);
                });
            }
        };

        ProgressManager.getInstance().run(t);
    }

    @Override
    public void projectClosed(@NotNull Project project)
    {
        _subscribers.forEach(s ->
        {
            ICogniCryptProjectListener value = s.get();
            if (value != null)
                value.OnProjectClosed(project);
        });
    }

    @Override
    public void projectClosing(@NotNull Project project)
    {
        _subscribers.forEach(s ->
        {
            ICogniCryptProjectListener value = s.get();
            if (value != null)
                value.OnProjectClosing(project);
        });
    }

    public IReadOnlyCollection<Project> GetOpenProject()
    {
        return new ReadOnlyCollection<>(_projects);
    }

    public void Subscribe(ICogniCryptProjectListener subscriber)
    {
        _subscribers.add(new WeakReference<>(subscriber));
    }

    public void UnSubscribe(ICogniCryptProjectListener subscriber)
    {
        _subscribers.remove(subscriber);
    }
}

