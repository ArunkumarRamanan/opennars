package vnc.viewer.swing;

import vnc.rfb.protocol.Protocol;
import vnc.rfb.protocol.ProtocolSettings;
import vnc.viewer.ConnectionPresenter;
import vnc.viewer.UiSettings;
import vnc.VNCClient;

/**
 * @author dime at tightvnc.com
 */
public class SwingViewerWindowFactory {

    private final boolean isSeparateFrame;
    private final boolean isApplet;
    private final VNCClient viewer;

    public SwingViewerWindowFactory(boolean isSeparateFrame, boolean isApplet, VNCClient viewer) {
        this.isSeparateFrame = isSeparateFrame;
        this.isApplet = isApplet;
        this.viewer = viewer;
    }

    public SwingViewerWindow createViewerWindow(Protocol workingProtocol,
                                                ProtocolSettings rfbSettings, UiSettings uiSettings,
                                                String connectionString, ConnectionPresenter presenter) {
        Surface surface = new Surface(workingProtocol, uiSettings.getScaleFactor(), uiSettings.getMouseCursorShape());
        final SwingViewerWindow viewerWindow = new SwingViewerWindow(workingProtocol, rfbSettings, uiSettings,
                surface, isSeparateFrame, isApplet, viewer, connectionString, presenter);
        surface.setViewerWindow(viewerWindow);
        viewerWindow.setRemoteDesktopName(workingProtocol.getRemoteDesktopName());
        rfbSettings.addListener(viewerWindow);
        uiSettings.addListener(surface);
        return viewerWindow;
    }

}
