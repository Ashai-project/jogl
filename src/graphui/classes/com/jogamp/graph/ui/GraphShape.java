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
package com.jogamp.graph.ui;

import java.util.ArrayList;
import java.util.List;

import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.GLRegion;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.math.Vec4f;
import com.jogamp.opengl.util.texture.TextureSequence;

/**
 * Graph based {@link GLRegion} {@link Shape}
 * <p>
 * GraphUI is GPU based and resolution independent.
 * </p>
 * <p>
 * GraphUI is intended to become an immediate- and retained-mode API.
 * </p>
 * @see Scene
 */
public abstract class GraphShape extends Shape {
    protected final int renderModes;
    protected GLRegion region = null;
    protected float oshapeSharpness = OutlineShape.DEFAULT_SHARPNESS;
    private int regionQuality = Region.MAX_QUALITY;
    private final List<GLRegion> dirtyRegions = new ArrayList<GLRegion>();

    /**
     * Create a Graph based {@link GLRegion} UI {@link Shape}.
     *
     * @param renderModes Graph's {@link Region} render modes, see {@link GLRegion#create(GLProfile, int, TextureSequence) create(..)}.
     */
    public GraphShape(final int renderModes) {
        super();
        this.renderModes = renderModes;
    }

    /** Return Graph's {@link Region} render modes, see {@link GLRegion#create(GLProfile, int, TextureSequence) create(..)}. */
    public final int getRenderModes() { return renderModes; }

    public final int getQuality() { return regionQuality; }
    public final GraphShape setQuality(final int q) {
        this.regionQuality = q;
        if( null != region ) {
            region.setQuality(q);
        }
        return this;
    }
    public final GraphShape setSharpness(final float sharpness) {
        this.oshapeSharpness = sharpness;
        markShapeDirty();
        return this;
    }
    public final float getSharpness() {
        return oshapeSharpness;
    }

    @Override
    public boolean hasColorChannel() {
        return Region.hasColorChannel(renderModes) || Region.hasColorTexture(renderModes);
    }

    private final void clearDirtyRegions(final GL2ES2 gl) {
        for(final GLRegion r : dirtyRegions) {
            r.destroy(gl);
        }
        dirtyRegions.clear();
    }

    @Override
    protected final void clearImpl0(final GL2ES2 gl, final RegionRenderer renderer) {
        clearImpl(gl, renderer);
        clearDirtyRegions(gl);
        if( null != region ) {
            region.clear(gl);
        }
    }

    @Override
    protected final void destroyImpl0(final GL2ES2 gl, final RegionRenderer renderer) {
        destroyImpl(gl, renderer);
        clearDirtyRegions(gl);
        if( null != region ) {
            region.destroy(gl);
            region = null;
        }
    }

    @Override
    protected final void drawImpl0(final GL2ES2 gl, final RegionRenderer renderer, final int[] sampleCount, final Vec4f rgba) {
        if( null != rgba ) {
            renderer.getRenderState().setColorStatic(rgba);
        }
        region.draw(gl, renderer, sampleCount);
    }

    /**
     * Update or freshly create the {@link GLRegion}, while allocating its buffers with given initial `vertexCount` and `indexCount`.
     *
     * Method shall be invoked by the {@link #addShapeToRegion(GLProfile, GL2ES2)} implementation
     * before actually adding the {@link OutlineShape} to the {@link GLRegion}.
     *
     * {@link #addShapeToRegion(GLProfile, GL2ES2)} is capable to determine initial `vertexCount` and `indexCount` buffer sizes,
     * as it composes the {@link OutlineShape}s to be added.
     *
     * {@link #updateGLRegion(GLProfile, GL2ES2, TextureSequence, OutlineShape)} maybe used for convenience.
     *
     * In case {@link GLRegion} is `null`, a new instance is being created.
     *
     * In case the {@link GLRegion} already exists, it will be either {@link GLRegion#clear(GL2ES2) cleared} if the {@link GL2ES2} `gl`
     * instance is not `null` or earmarked for deletion at a later time and a new instance is being created.
     *
     * @param glp the used GLProfile, never `null`
     * @param gl the optional current {@link GL2ES2} instance, maybe `null`.
     * @param colorTexSeq optional {@link TextureSequence} for {@link Region#COLORTEXTURE_RENDERING_BIT} rendering mode.
     * @param vertexCount the initial {@link GLRegion} vertex buffer size
     * @param indexCount the initial {@link GLRegion} index buffer size
     * @see #updateGLRegion(GLProfile, GL2ES2, TextureSequence, OutlineShape)
     */
    protected void updateGLRegion(final GLProfile glp, final GL2ES2 gl, final TextureSequence colorTexSeq, final int vertexCount, final int indexCount) {
        if( null == region ) {
            region = GLRegion.create(glp, renderModes, colorTexSeq, vertexCount, indexCount);
        } else if( null == gl ) {
            dirtyRegions.add(region);
            region = GLRegion.create(glp, renderModes, colorTexSeq, vertexCount, indexCount);
        } else {
            region.clear(gl);
            region.setBufferCapacity(vertexCount, indexCount);
        }
    }
    /**
     * Convenient {@link #updateGLRegion(GLProfile, GL2ES2, TextureSequence, int, int)} variant determining initial
     * {@link GLRegion} buffer sizes via {@link Region#countOutlineShape(OutlineShape, int[])}.
     *
     * @param glp the used GLProfile, never `null`
     * @param gl the optional current {@link GL2ES2} instance, maybe `null`.
     * @param colorTexSeq optional {@link TextureSequence} for {@link Region#COLORTEXTURE_RENDERING_BIT} rendering mode.
     * @param shape the {@link OutlineShape} used to determine {@link GLRegion}'s buffer sizes via {@link Region#countOutlineShape(OutlineShape, int[])}
     * @see #updateGLRegion(GLProfile, GL2ES2, TextureSequence, int, int)
     */
    protected void updateGLRegion(final GLProfile glp, final GL2ES2 gl, final TextureSequence colorTexSeq, final OutlineShape shape) {
        final int[/*2*/] vertIndexCount = Region.countOutlineShape(shape, new int[2]);
        updateGLRegion(glp, gl, colorTexSeq, vertIndexCount[0], vertIndexCount[1]);
    }

    @Override
    protected final void validateImpl(final GLProfile glp, final GL2ES2 gl) {
        if( null != gl ) {
            clearDirtyRegions(gl);
        }
        if( isShapeDirty() ) {
            addShapeToRegion(glp, gl); // calls updateGLRegion(..)
            if( hasBorder() ) {
                addBorderOutline();
            }
            region.setQuality(regionQuality);
        } else if( isStateDirty() ) {
            region.markStateDirty();
        }
    }

    protected void addBorderOutline() {
        final OutlineShape shape = new OutlineShape();
        final float x1 = box.getMinX();
        final float x2 = box.getMaxX();
        final float y1 = box.getMinY();
        final float y2 = box.getMaxY();
        final float z = box.getCenter().z(); // 0; // box.getMinZ() + 0.025f;
        {
            // Outer OutlineShape as Winding.CCW.
            shape.moveTo(x1, y1, z);
            shape.lineTo(x2, y1, z);
            shape.lineTo(x2, y2, z);
            shape.lineTo(x1, y2, z);
            shape.lineTo(x1, y1, z);
            shape.closeLastOutline(true);
            shape.addEmptyOutline();
        }
        {
            // Inner OutlineShape as Winding.CW.
            final float dxy0 = box.getWidth() < box.getHeight() ? box.getWidth() : box.getHeight();
            final float dxy = dxy0 * getBorderThickness();
            shape.moveTo(x1+dxy, y1+dxy, z);
            shape.lineTo(x1+dxy, y2-dxy, z);
            shape.lineTo(x2-dxy, y2-dxy, z);
            shape.lineTo(x2-dxy, y1+dxy, z);
            shape.lineTo(x1+dxy, y1+dxy, z);
            shape.closeLastOutline(true);
        }
        shape.setIsQuadraticNurbs();
        shape.setSharpness(oshapeSharpness);
        region.addOutlineShape(shape, null, borderColor);
    }

    protected void clearImpl(final GL2ES2 gl, final RegionRenderer renderer) { }

    protected void destroyImpl(final GL2ES2 gl, final RegionRenderer renderer) { }

    protected abstract void addShapeToRegion(GLProfile glp, GL2ES2 gl);

}
