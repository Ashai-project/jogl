/**
 * Copyright 2010-2023 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */
package com.jogamp.opengl.demos.graph.ui;

import com.jogamp.nativewindow.ScalableSurface;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.JoglVersion;
import com.jogamp.opengl.demos.util.MiscUtils;
import com.jogamp.common.util.VersionUtil;
import com.jogamp.graph.curve.Region;
import com.jogamp.newt.Display;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.caps.NonFSAAGLCapsChooser;

public class GPUUISceneNewtDemo {
    static final boolean DEBUG = false;
    static final boolean TRACE = false;

    static void sleep(final long ms) {
        try {
            Thread.sleep(ms);
        } catch (final InterruptedException ie) {}
    }

    public static void main(final String[] args) {
        int sceneMSAASamples = 0;
        boolean graphVBAAMode = true;
        boolean graphMSAAMode = false;
        float graphAutoMode = 0; // GPUUISceneGLListener0A.DefaultNoAADPIThreshold;

        final float[] reqSurfacePixelScale = new float[] { ScalableSurface.AUTOMAX_PIXELSCALE, ScalableSurface.AUTOMAX_PIXELSCALE };

        String fontfilename = null;
        String filmURL = null;

        int width = 1280, height = 720;

        boolean forceES2 = false;
        boolean forceES3 = false;
        boolean forceGL3 = false;
        boolean forceGLDef = false;

        if( 0 != args.length ) {
            for(int i=0; i<args.length; i++) {
                if(args[i].equals("-gnone")) {
                    sceneMSAASamples = 0;
                    graphMSAAMode = false;
                    graphVBAAMode = false;
                    graphAutoMode = 0f;
                } else if(args[i].equals("-smsaa")) {
                    i++;
                    sceneMSAASamples = MiscUtils.atoi(args[i], sceneMSAASamples);
                    graphMSAAMode = false;
                    graphVBAAMode = false;
                    graphAutoMode = 0f;
                } else if(args[i].equals("-gmsaa")) {
                    graphMSAAMode = true;
                    graphVBAAMode = false;
                    graphAutoMode = 0f;
                } else if(args[i].equals("-gvbaa")) {
                    graphMSAAMode = false;
                    graphVBAAMode = true;
                    graphAutoMode = 0f;
                } else if(args[i].equals("-gauto")) {
                    graphMSAAMode = false;
                    graphVBAAMode = true;
                    i++;
                    graphAutoMode = MiscUtils.atof(args[i], graphAutoMode);
                } else if(args[i].equals("-font")) {
                    i++;
                    fontfilename = args[i];
                } else if(args[i].equals("-width")) {
                    i++;
                    width = MiscUtils.atoi(args[i], width);
                } else if(args[i].equals("-height")) {
                    i++;
                    height = MiscUtils.atoi(args[i], height);
                } else if(args[i].equals("-pixelScale")) {
                    i++;
                    final float pS = MiscUtils.atof(args[i], reqSurfacePixelScale[0]);
                    reqSurfacePixelScale[0] = pS;
                    reqSurfacePixelScale[1] = pS;
                } else if(args[i].equals("-es2")) {
                    forceES2 = true;
                } else if(args[i].equals("-es3")) {
                    forceES3 = true;
                } else if(args[i].equals("-gl3")) {
                    forceGL3 = true;
                } else if(args[i].equals("-gldef")) {
                    forceGLDef = true;
                } else if(args[i].equals("-film")) {
                    i++;
                    filmURL = args[i];
                }
            }
        }
        System.err.println("forceES2   "+forceES2);
        System.err.println("forceES3   "+forceES3);
        System.err.println("forceGL3   "+forceGL3);
        System.err.println("forceGLDef "+forceGLDef);
        System.err.println("Desired win size "+width+"x"+height);
        System.err.println("Scene MSAA Samples "+sceneMSAASamples);
        System.err.println("Graph MSAA Mode "+graphMSAAMode);
        System.err.println("Graph VBAA Mode "+graphVBAAMode);
        System.err.println("Graph Auto Mode "+graphAutoMode+" no-AA dpi threshold");

        final Display dpy = NewtFactory.createDisplay(null);
        final Screen screen = NewtFactory.createScreen(dpy, 0);
        System.err.println(VersionUtil.getPlatformInfo());
        System.err.println(JoglVersion.getAllAvailableCapabilitiesInfo(dpy.getGraphicsDevice(), null).toString());

        final GLProfile glp;
        if(forceGLDef) {
            glp = GLProfile.getDefault();
        } else if(forceGL3) {
            glp = GLProfile.get(GLProfile.GL3);
        } else if(forceES3) {
            glp = GLProfile.get(GLProfile.GLES3);
        } else if(forceES2) {
            glp = GLProfile.get(GLProfile.GLES2);
        } else {
            glp = GLProfile.getGL2ES2();
        }
        System.err.println("GLProfile: "+glp);
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setAlphaBits(4);
        if( sceneMSAASamples > 0 ) {
            caps.setSampleBuffers(true);
            caps.setNumSamples(sceneMSAASamples);
        }
        System.out.println("Requested: " + caps);

        final int renderModes;
        if( graphVBAAMode ) {
            renderModes = Region.VBAA_RENDERING_BIT;
        } else if( graphMSAAMode ) {
            renderModes = Region.MSAA_RENDERING_BIT;
        } else {
            renderModes = 0;
        }

        final GLWindow window = GLWindow.create(screen, caps);
        if( 0 == sceneMSAASamples ) {
            window.setCapabilitiesChooser(new NonFSAAGLCapsChooser(true));
        }
        window.setSize(width, height);
        window.setTitle("GraphUI Newt Demo: graph["+Region.getRenderModeString(renderModes)+"], msaa "+sceneMSAASamples);
        window.setSurfaceScale(reqSurfacePixelScale);
        // final float[] valReqSurfacePixelScale = window.getRequestedSurfaceScale(new float[2]);

        final GPUUISceneGLListener0A scene = 0 < graphAutoMode ? new GPUUISceneGLListener0A(fontfilename, filmURL, graphAutoMode, DEBUG, TRACE) :
                                                                 new GPUUISceneGLListener0A(fontfilename, filmURL, renderModes, DEBUG, TRACE);
        window.addGLEventListener(scene);

        final Animator animator = new Animator();
        animator.setUpdateFPSFrames(5*60, null);
        animator.add(window);

        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowDestroyed(final WindowEvent e) {
                animator.stop();
            }
        });

        window.setVisible(true);
        animator.start();

        // sleep(3000);
        // final UIShape movie = scene.getWidget(GPUUISceneGLListener0A.BUTTON_MOVIE);
    }

}
