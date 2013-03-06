/**
 * 
 */
package rajawali.renderer.plugins;

import java.util.Stack;

import android.graphics.Bitmap.Config;
import android.opengl.GLES20;

import rajawali.Camera;
import rajawali.materials.TextureInfo;
import rajawali.materials.TextureManager.FilterType;
import rajawali.materials.TextureManager.TextureType;
import rajawali.materials.TextureManager.WrapType;
import rajawali.math.Number3D;
import rajawali.math.Vector2D;
import rajawali.renderer.RajawaliRenderer;
import rajawali.extras.LensFlare;
import rajawali.extras.LensFlare.FlareInfo;


/**
 * @author Andrew Jo
 *
 */
public final class LensFlarePlugin extends Plugin {
	private static final String mVShaderVertexTexture =
			"precision highp float;\n" +
			"\n" +
			"uniform lowp int uRenderType;\n" +
			"uniform vec3 uScreenPosition;\n" +
			"uniform vec2 uScale;\n" +
			"uniform float uRotation;\n" +
			"uniform sampler2D uOcclusionMap;\n" +
			"\n" +
			"attribute vec2 aPosition;\n" +
			"attribute vec2 aTextureCoord;\n" +
			"\n" +
			"varying vec2 vTextureCoord;\n" +
			"varying float vVisibility;\n" +
			"\n" +
			"void main() {\n" +
			"   vTextureCoord = aTextureCoord;\n" +
			"\n" +
			"   vec2 pos = aPosition;\n" +
			"\n" +
			"   if (uRenderType == 3) {\n" +
			"      vec4 visibility = texture2D(uOcclusionMap, vec2(0.1, 0.1)) +\n" +
			"                        texture2D(uOcclusionMap, vec2(0.5, 0.1)) +\n" +
			"                        texture2D(uOcclusionMap, vec2(0.9, 0.1)) +\n" +
			"                        texture2D(uOcclusionMap, vec2(0.9, 0.5)) +\n" +
			"                        texture2D(uOcclusionMap, vec2(0.9, 0.9)) +\n" +
			"                        texture2D(uOcclusionMap, vec2(0.5, 0.9)) +\n" +
			"                        texture2D(uOcclusionMap, vec2(0.1, 0.9)) +\n" +
			"                        texture2D(uOcclusionMap, vec2(0.1, 0.5)) +\n" +
			"                        texture2D(uOcclusionMap, vec2(0.5, 0.5));\n" +
			"\n" +
			"      vVisibility = (visibility.r / 9.0) * (1.0 - visibility.g / 9.0) *\n" +
			"                    (visibility.b / 9.0) * (1.0 - visibility.a / 9.0);\n" +
			"\n" +
			"      pos.x = cos(uRotation) * aPosition.x - sin(uRotation) * aPosition.y;\n" +
			"      pos.y = sin(uRotation) * aPosition.x + cos(uRotation) * aPosition.y;\n" +
			"   }\n" +
			"\n" +
			"   gl_Position = vec4((pos * uScale + uScreenPosition.xy).xy, uScreenPosition.z, 1.0);\n" +
			"}";
	
	private static final String mFShaderVertexTexture =
			"precision highp float;\n" +
	        "\n" +
	        "uniform lowp int uRenderType;\n" +
	        "uniform sampler2D uMap;\n" +
	        "uniform float uOpacity;\n" +
	        "uniform vec3 uColor;\n" +
	        "\n" +
	        "varying vec2 vTextureCoord;\n" +
	        "varying float vVisibility;\n" +
	        "\n" +
	        "void main() {\n" +
	        "   if (uRenderType == 1) {\n" +
	        "      gl_FragColor = vec4(1.0, 0.0, 1.0, 0.0);\n" +
	        "   } else if (uRenderType == 2) {\n" +
	        "      gl_FragColor = texture2D(uMap, vTextureCoord);\n" +
	        "   } else {\n" +
	        "      vec4 texture = texture2D(uMap, vTextureCoord);\n" +
	        "      texture.a *= uOpacity * vVisibility;\n" +
	        "      gl_FragColor = texture;\n" +
	        "      gl_FragColor.rgb *= uColor;\n" +
	        "   }\n" +
	        "}";
			
	private static final String mVShaderNoVertexTexture =
			"precision highp float;\n" +
			"\n" +
			"uniform mediump int uRenderType;\n" +
			"uniform vec3 uScreenPosition;\n" +
			"uniform float uRotation;\n" +
			"uniform vec2 uScale;\n" +
			"\n" +
			"attribute vec2 aPosition;\n" +
			"attribute vec2 aTextureCoord;\n" +
			"\n" +
			"varying vec2 vTextureCoord;\n" +
			"\n" +
			"void main() {\n" +
			"   vTextureCoord = aTextureCoord;\n" +
			"   vec2 pos = aPosition;\n" +
			"   if (uRenderType == 3) {\n" +
			"      pos.x = cos(uRotation) * aPosition.x - sin(uRotation) * aPosition.y;\n" +
			"      pos.y = sin(uRotation) * aPosition.x + cos(uRotation) * aPosition.y;\n" +
			"   }\n" +
			"   gl_Position = vec4((pos * uScale + uScreenPosition.xy).xy, uScreenPosition.z, 1.0);\n" +
			"}";
	
	private static final String mFShaderNoVertexTexture =
			"precision mediump float;\n" +
			"\n" +
			"uniform mediump int uRenderType;\n" +
			"uniform lowp int uDebugMode;\n" +
			"uniform mediump sampler2D uMap;\n" +
			"uniform mediump sampler2D uOcclusionMap;\n" +
			"uniform mediump sampler2D uFlareTexture;\n" +
			"uniform float uOpacity;\n" +
			"uniform vec3 uColor;\n" +
			"\n" +
			"varying vec2 vTextureCoord;\n" +
			"\n" +
			"void main() {\n" +
			"   if (uRenderType == 1) {\n" +
			"      gl_FragColor = vec4(1.0, 0.0, 1.0, 0.0);\n" +
			"   } else if (uRenderType == 2) {\n" +
			"      gl_FragColor = texture2D(uMap, vTextureCoord);\n" +
			"   } else {\n" +
			"      vec4 visibility = texture2D(uOcclusionMap, vec2(0.5, 0.1)) +\n" +
			"                        texture2D(uOcclusionMap, vec2(0.9, 0.5)) +\n" +
			"                        texture2D(uOcclusionMap, vec2(0.5, 0.9)) +\n" +
			"                        texture2D(uOcclusionMap, vec2(0.1, 0.5));\n" +
			"      float finalVisibility = (visibility.r / 4.0) * (1.0 - visibility.g / 4.0) *\n" +
			"                              (visibility.b / 4.0) * (1.0 - visibility.a / 4.0);\n" +
			//"      finalVisibility = 0.5;\n" +*/ 
			/*"      float finalVisibility = texture2D(uOcclusionMap, vec2(0.5, 0.1)).a +\n" +
			"                              texture2D(uOcclusionMap, vec2(0.9, 0.5)).a +\n" +
			"                              texture2D(uOcclusionMap, vec2(0.5, 0.9)).a +\n" +
			"                              texture2D(uOcclusionMap, vec2(0.1, 0.5)).a;\n" +
			"      finalVisibility = (1.0 - finalVisibility / 4.0);\n" +*/
			"\n" +
			"      vec4 texture = texture2D(uMap, vTextureCoord);\n" +
			"      if (uDebugMode == 1) {\n" +
			"         texture.a = 1.0;\n" +
			"      } else {\n" +
			"         texture.a *= uOpacity * finalVisibility;\n" +
			"      }" +
			"      gl_FragColor = texture;\n" +
			"      gl_FragColor.rgb *= uColor;\n" +
			"   }\n" +
			"}";
	
	private Stack<LensFlare> mLensFlares;
	
	/*
	 * Determines whether texture sampling is supported in the vertex shader.
	 */
	private boolean mVertexTextureSupported;
	
	/*
	 * Shader Attribute Handles
	 */
	private int maPositionHandle;
	private int maTextureCoordHandle;
	
	/*
	 * Shader Uniform Handles
	 */
	private int muRenderTypeHandle;
	private int muRotationHandle;
	private int muScreenPositionHandle;
	private int muOpacityHandle;
	private int muScaleHandle;
	private int muColorHandle;
	private int muMapTextureHandle;
	private int muOcclusionMapTextureHandle;
	// private int muDebugModeHandle; // UNCOMMENT TO USE DEBUG MODE
	
	private TextureInfo mMapTexture;
	private TextureInfo mOcclusionMapTexture;
	
	public LensFlarePlugin(RajawaliRenderer renderer) {
		super(renderer);
	}
	
	@Override
	protected void init() {
		mLensFlares = new Stack<LensFlare>();
		int[] maxVertexTextureImageUnits = new int[1]; 
		GLES20.glGetIntegerv(GLES20.GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS, maxVertexTextureImageUnits, 0);
		mVertexTextureSupported = maxVertexTextureImageUnits[0] != 0;
		
		int i = 0, j = 0;
        int numVertices = 8;
        float[] vertices = new float[numVertices];
        float[] textureCoords = new float[numVertices];
        float[] normals = new float[numVertices * 3];
		float[] colors = new float[numVertices * 4];
        int[] indices = new int[6];
        int vertexCount = 0;
        int texCoordCount = 0;
        
        // Create a quad with vertices at (-1, -1), (1, -1), (1, 1), (-1, 1).
        vertices[vertexCount++] = -1;	vertices[vertexCount++] = -1;
        vertices[vertexCount++] = 1;	vertices[vertexCount++] = -1;
        vertices[vertexCount++] = 1;	vertices[vertexCount++] = 1;
        vertices[vertexCount++] = -1;	vertices[vertexCount++] = 1;
        
        // Each vertex normals are facing upwards on the Y-axis.
        for (j = 0; j < numVertices; j+=3) {
        	normals[j] = 0;
        	normals[j+1] = 1;
        	normals[j+2] = 0;
        }
        
        // Texture (UV) coordinates are at (0, 0), (1, 0), (1, 1), (0, 1).
        textureCoords[texCoordCount++] = 0;	textureCoords[texCoordCount++] = 0;
        textureCoords[texCoordCount++] = 1;	textureCoords[texCoordCount++] = 0;
        textureCoords[texCoordCount++] = 1;	textureCoords[texCoordCount++] = 1;
        textureCoords[texCoordCount++] = 0;	textureCoords[texCoordCount++] = 1;
        
        // Draw the vertices in the index order of 0 -> 1 -> 2 -> 0 -> 2 -> 3.
        indices[i++] = 0;	indices[i++] = 1;	indices[i++] = 2;
        indices[i++] = 0;	indices[i++] = 2;	indices[i++] = 3;
        
        // Just default to yellow.
        for (i = 0; i < colors.length; i+=4) {
        	colors[i] = 1.0f;
        	colors[i+1] = 1.0f;
        	colors[i+2] = 0.0f;
        	colors[i+3] = 1.0f;
        }
        
        // Set geometry data.
        setData(vertices, normals, textureCoords, colors, indices);
        
        // Set up lookup textures.
        mMapTexture = mRenderer.getTextureManager().addTexture(null, null, 16, 16, TextureType.LOOKUP, Config.RGB_565, false, false, WrapType.CLAMP, FilterType.NEAREST);
        mOcclusionMapTexture = mRenderer.getTextureManager().addTexture(null, null, 16, 16, TextureType.LOOKUP, Config.ARGB_8888, false, false, WrapType.CLAMP, FilterType.NEAREST);
        
        // Set up shader program.
        if (mVertexTextureSupported)
        	setShaders(mVShaderVertexTexture, mFShaderVertexTexture);
        else
	        setShaders(mVShaderNoVertexTexture, mFShaderNoVertexTexture);
	}

	public void addLensFlare(LensFlare lensFlare) {
		mLensFlares.add(lensFlare);
	}
	
	public int getLensFlareCount() {
		return mLensFlares.size();
	}
	
	public boolean removeLensFlare(LensFlare lensFlare) {
		return mLensFlares.remove(lensFlare);
	}
	
	@Override
	public void render() {
		int f, i, j;
		// Calculate world space position to normalized screen space.
		float viewportWidth = mRenderer.getViewportWidth(), viewportHeight = mRenderer.getViewportHeight();
		float invAspect = viewportHeight / viewportWidth;
		float size;
		Vector2D scale = new Vector2D();
		float halfViewportWidth = viewportWidth / 2;
		float halfViewportHeight = viewportHeight / 2;
		Number3D screenPosition = new Number3D();
		Vector2D screenPositionPixels = new Vector2D();
		Camera camera = mRenderer.getCamera();
		float[] viewMatrix = camera.getViewMatrix(), projMatrix = camera.getProjectionMatrix();
		
		useProgram(mProgram);
		
		// Push the VBOs to the GPU.
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mGeometry.getVertexBufferInfo().bufferHandle);
		GLES20.glEnableVertexAttribArray(maPositionHandle);
		fix.android.opengl.GLES20.glVertexAttribPointer(maPositionHandle, 2, GLES20.GL_FLOAT, false, 0, 0);
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
		
		// Push texture coordinates to the GPU.
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mGeometry.getTexCoordBufferInfo().bufferHandle);
		GLES20.glEnableVertexAttribArray(maTextureCoordHandle);
		fix.android.opengl.GLES20.glVertexAttribPointer(maTextureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, 0);
		
		// Push vertex element indices to the GPU.
		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mGeometry.getIndexBufferInfo().bufferHandle);
		
		// Set up texture locations.
		GLES20.glUniform1i(muOcclusionMapTextureHandle, 0);
		GLES20.glUniform1i(muMapTextureHandle, 1);
		
		GLES20.glDisable(GLES20.GL_CULL_FACE);
		GLES20.glDepthMask(false);
		
		synchronized (mLensFlares) {
			for (i = 0, j = mLensFlares.size(); i < j; i++) {
				size = 16 / viewportHeight;
				scale.x = size * invAspect;
				scale.y = size;
				
				LensFlare lensFlare = mLensFlares.get(i);
				
				// Calculate normalized device coordinates.
				screenPosition.setAllFrom(lensFlare.getPosition());
				screenPosition.multiply(viewMatrix);
				screenPosition.project(projMatrix);
				
				// Calculate actual device coordinates.
				screenPositionPixels.x = screenPosition.x * halfViewportWidth + halfViewportWidth;
				screenPositionPixels.y = screenPosition.y * halfViewportHeight + halfViewportHeight;
				
				// Calculate the angle between the camera and the light vector.
				Number3D lightSourceDirection = new Number3D(lensFlare.getPosition());
				lightSourceDirection.normalize();
				Number3D cameraPosition = new Number3D(camera.getPosition());
				Number3D cameraLookAt = new Number3D(camera.getLookAt());
				Number3D cameraDirection = new Number3D(cameraLookAt.subtract(cameraPosition));
				cameraDirection.normalize();
				float angleLightToCamera = cameraDirection.dot(lightSourceDirection);
				
				// Camera needs to be facing towards the light and the light should come within the
				// viewing frustum.
				if (angleLightToCamera > 0 &&
						screenPositionPixels.x > 0 && screenPositionPixels.x < viewportWidth &&
						screenPositionPixels.y > 0 && screenPositionPixels.y < viewportHeight) {
					// Bind current framebuffer to texture.
					GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
					GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mMapTexture.getTextureId());
					GLES20.glCopyTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGB, 
							(int)screenPositionPixels.x - 8, (int)screenPositionPixels.y - 8, 16, 16, 0);
					
					// First render pass.
					GLES20.glUniform1i(muRenderTypeHandle, 1);
					GLES20.glUniform2fv(muScaleHandle, 1, new float[] { scale.x, scale.y }, 0);
					GLES20.glUniform3fv(muScreenPositionHandle, 1, new float[] { screenPosition.x, screenPosition.y, screenPosition.z }, 0);
					
					GLES20.glDisable(GLES20.GL_BLEND);
					GLES20.glEnable(GLES20.GL_DEPTH_TEST);
					
					fix.android.opengl.GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, 
							mGeometry.areOnlyShortBuffersSupported() ? GLES20.GL_UNSIGNED_SHORT : GLES20.GL_UNSIGNED_INT, 
							0);
					
					// Copy result to occlusion map.
					GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
					GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mOcclusionMapTexture.getTextureId());
					GLES20.glCopyTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, 
							(int)screenPositionPixels.x - 8, (int)screenPositionPixels.y - 8, 16, 16, 0);
					
					// Second render pass.
					GLES20.glUniform1i(muRenderTypeHandle, 2);
					GLES20.glDisable(GLES20.GL_DEPTH_TEST);
					
					GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
					GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mMapTexture.getTextureId());
					fix.android.opengl.GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, 
							mGeometry.areOnlyShortBuffersSupported() ? GLES20.GL_UNSIGNED_SHORT : GLES20.GL_UNSIGNED_INT, 
							0);
					
					// Update the flare's screen positions.
					lensFlare.setPositionScreen(screenPosition);
					lensFlare.updateLensFlares();
					
					// Third render pass.
					GLES20.glUniform1i(muRenderTypeHandle, 3);
					GLES20.glEnable(GLES20.GL_BLEND);
					GLES20.glBlendEquation(GLES20.GL_FUNC_ADD);
					GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE);
					
					// DEBUG - Shows the current uMap and uOcclusionMap textures.
					// NOTE: UNCOMMENT IF THE LENS FLARE DOES NOT GET OCCLUDED.
					// IF THE OCCLUSION TEXTURE IS EMPTY, YOU ARE USING RGB_565 IN YOUR EGL CONFIG.
					// SWITCH TO RGBA_8888.
					/*
					GLES20.glUniform3fv(muScreenPositionHandle, 1, new float[] { -0.75f, -0.35f, 0 }, 0);
					GLES20.glUniform2fv(muScaleHandle, 1, new float[] { (200 / viewportHeight) * invAspect, 200 / viewportHeight }, 0);
					GLES20.glUniform1f(muRotationHandle, 0);
					GLES20.glUniform1i(muDebugModeHandle, 1);
					GLES20.glUniform1f(muOpacityHandle, 1);
					GLES20.glUniform3fv(muColorHandle, 1, new float[] { 1, 1, 1 }, 0);
					GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
					GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mMapTexture.getTextureId());
					fix.android.opengl.GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, mGeometry.areOnlyShortBuffersSupported() ? GLES20.GL_UNSIGNED_SHORT : GLES20.GL_UNSIGNED_INT, 0);
					GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
					GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
					GLES20.glUniform3fv(muScreenPositionHandle, 1, new float[] { -0.3f, -0.35f, 0 }, 0);
					GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
					GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mOcclusionMapTexture.getTextureId());
					fix.android.opengl.GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, mGeometry.areOnlyShortBuffersSupported() ? GLES20.GL_UNSIGNED_SHORT : GLES20.GL_UNSIGNED_INT, 0);
					GLES20.glUniform1i(muDebugModeHandle, 0);
					GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
					GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
					*/
					// END DEBUG
					
					for (f = 0; f < lensFlare.getLensFlares().size(); f++) {
						FlareInfo sprite = lensFlare.getLensFlares().get(f);
						// Don't bother rendering if the sprite's too transparent or too small.
						if (sprite.getOpacity() > 0.001f && sprite.getScale() > 0.001f) {
							screenPosition.setAllFrom(sprite.getScreenPosition());
							
							// Calculate pixel size to normalized size
							size = sprite.getSize() * sprite.getScale() / viewportHeight;
							
							scale.x = size * invAspect;
							scale.y = size;
							
							GLES20.glUniform3fv(muScreenPositionHandle, 1, new float[] { screenPosition.x, screenPosition.y, screenPosition.z }, 0);
							GLES20.glUniform2fv(muScaleHandle, 1, new float[] { scale.x, scale.y }, 0);
							GLES20.glUniform1f(muRotationHandle, sprite.getRotation());
							
							GLES20.glUniform1f(muOpacityHandle, sprite.getOpacity());
							GLES20.glUniform3fv(muColorHandle, 1, new float[] { sprite.getColor().x, sprite.getColor().y, sprite.getColor().z }, 0);
							
							GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
							GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, sprite.getTexture().getTextureId());
							
							GLES20.glEnable(GLES20.GL_BLEND);
							GLES20.glBlendEquation(GLES20.GL_FUNC_ADD);
							GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE);
							
							// Draw the elements.
							fix.android.opengl.GLES20.glDrawElements(GLES20.GL_TRIANGLES,
									mGeometry.getNumIndices(),
									mGeometry.areOnlyShortBuffersSupported() ? GLES20.GL_UNSIGNED_SHORT : GLES20.GL_UNSIGNED_INT,
									0);
							
							// Unbind texture.
							GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
							GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
						}
					}
				}
			}
		}
		// Unbind element array.
		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
		GLES20.glEnable(GLES20.GL_CULL_FACE);
		GLES20.glEnable(GLES20.GL_DEPTH_TEST);
		GLES20.glDepthMask(true);
	}
	
	@Override
	protected void setShaders(String vertexShader, String fragmentShader) {
		super.setShaders(vertexShader, fragmentShader);
		
		maPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
		maTextureCoordHandle = GLES20.glGetAttribLocation(mProgram, "aTextureCoord");
		muRenderTypeHandle = GLES20.glGetUniformLocation(mProgram, "uRenderType");
		muScreenPositionHandle = GLES20.glGetUniformLocation(mProgram, "uScreenPosition");
		muRotationHandle = GLES20.glGetUniformLocation(mProgram, "uRotation");
		muScaleHandle = GLES20.glGetUniformLocation(mProgram, "uScale");
		muOpacityHandle = GLES20.glGetUniformLocation(mProgram, "uOpacity");
		muColorHandle = GLES20.glGetUniformLocation(mProgram, "uColor");
		muMapTextureHandle = GLES20.glGetUniformLocation(mProgram, "uMap");
		muOcclusionMapTextureHandle = GLES20.glGetUniformLocation(mProgram, "uOcclusionMap");
		// muDebugModeHandle = GLES20.glGetUniformLocation(mProgram, "uDebugMode"); // UNCOMMENT TO USE DEBUG MODE
	}
}
