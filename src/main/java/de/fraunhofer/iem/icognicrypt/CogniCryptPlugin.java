package de.fraunhofer.iem.icognicrypt;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.util.messages.MessageBusConnection;
import de.fraunhofer.iem.icognicrypt.IdeSupport.projects.CogniCryptProjectManager;
import de.fraunhofer.iem.icognicrypt.exceptions.CogniCryptException;
import de.fraunhofer.iem.icognicrypt.ui.CogniCryptToolWindowManager;

/**
 * This class gets initialized as soon as one project is loaded to the IDE.
 * It shall initialize global project-independent services.
 *
 * The attempt to create a second instance will cause an {@link CogniCryptException} to be thrown.
 *
 */
public class CogniCryptPlugin
{
    private static CogniCryptPlugin _instance;

    private MessageBusConnection _connection;

    public static CogniCryptPlugin GetInstance() throws CogniCryptException
    {
        if (_instance == null)
            throw new CogniCryptException("CogniCrypt is not initialized.");
        return _instance;
    }

    private CogniCryptPlugin() throws CogniCryptException
    {
        if (_instance != null)
            throw new CogniCryptException("CogniCrypt already initialized");
        _instance = this;
        Initialize();
    }

    public void Initialize()
    {
        CogniCryptProjectManager projectManager = ServiceManager.getService(CogniCryptProjectManager.class);
        CogniCryptToolWindowManager toolWindowManager = ServiceManager.getService(CogniCryptToolWindowManager.class);


        projectManager.Subscribe(toolWindowManager);

        // TODO: As CogniCrypt is executed on a totally different thread, check on which Thread the window is running and check if we can push data from this thread to the window.
    }
}
