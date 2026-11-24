package com.simha.app.ui.home.orb

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.view.Choreographer
import android.view.TextureView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

private const val VS = """
attribute vec2 pos;
void main() {
    gl_Position = vec4(pos, 0.0, 1.0);
}
"""

private const val FS = """
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif
uniform vec2 res;
uniform vec2 org;
uniform float rad;
uniform float t;
uniform float level;
uniform float dim;
uniform vec3 c0;
uniform vec3 c1;
uniform vec3 c2;
uniform vec3 c3;
uniform vec3 c4;

float hs(vec3 p) {
    p = fract(p * 0.3183099 + 0.1);
    p *= 17.0;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}

float vn(vec3 x) {
    vec3 i = floor(x);
    vec3 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    float a = mix(mix(hs(i), hs(i + vec3(1.0, 0.0, 0.0)), f.x),
                  mix(hs(i + vec3(0.0, 1.0, 0.0)), hs(i + vec3(1.0, 1.0, 0.0)), f.x), f.y);
    float b = mix(mix(hs(i + vec3(0.0, 0.0, 1.0)), hs(i + vec3(1.0, 0.0, 1.0)), f.x),
                  mix(hs(i + vec3(0.0, 1.0, 1.0)), hs(i + vec3(1.0, 1.0, 1.0)), f.x), f.y);
    return mix(a, b, f.z);
}

float fbm(vec3 x) {
    float a = 0.5;
    float s = 0.0;
    for (int i = 0; i < 4; i++) {
        s += a * vn(x);
        x = x * 2.02 + vec3(1.7, 9.2, 3.1);
        a *= 0.5;
    }
    return s;
}

void main() {
    vec2 fc = vec2(gl_FragCoord.x, res.y - gl_FragCoord.y);
    vec2 p = (fc - org) / rad;
    float r2 = dot(p, p);
    if (r2 > 1.0) {
        gl_FragColor = vec4(0.0);
        return;
    }
    float z = sqrt(1.0 - r2);
    vec3 n = vec3(p.x, p.y, z);
    float a = t * 1.0;
    vec3 q = n * 0.9;
    q = vec3(q.x * cos(a) + q.z * sin(a), q.y, -q.x * sin(a) + q.z * cos(a));
    float amp = 1.0 + level * 1.2;
    vec3 w = vec3(
        fbm(q + vec3(0.0, 0.0, t * 0.2)),
        fbm(q + vec3(5.2, 1.3, -t * 0.17)),
        fbm(q + vec3(2.4, 7.1, t * 0.13))
    );
    float f = fbm(q + (w - 0.5) * 2.0 * amp + vec3(0.0, t * 0.1, 0.0));
    float f2 = fbm(q * 1.5 - w * 1.5 + 3.0);
    vec3 col = mix(c1, c2, smoothstep(0.25, 0.75, f));
    col = mix(col, c0, (1.0 - smoothstep(0.25, 0.65, f2)) * 0.4);
    col = mix(col, c3, smoothstep(0.50, 0.85, w.x) * 0.5);
    col = mix(col, c4, smoothstep(0.55, 0.90, w.y) * 0.6);
    vec3 l = normalize(vec3(-0.5, -0.6, 0.65));
    float diff = clamp(dot(n, l), 0.0, 1.0);
    col *= 0.84 + 0.22 * diff;
    float rim = pow(1.0 - z, 2.2);
    vec2 dir = p / (sqrt(r2) + 0.0001);
    col = mix(col, vec3(0.788, 0.510, 0.180), rim * (0.15 + 0.5 * max(dot(dir, vec2(0.7071, 0.7071)), 0.0)));
    vec3 h = normalize(l + vec3(0.0, 0.0, 1.0));
    float nh = max(dot(n, h), 0.0);
    col += vec3(1.0, 0.99, 0.96) * (pow(nh, 40.0) * 0.55 + pow(nh, 6.0) * 0.14);
    col *= 1.0 - 0.18 * dim;
    col += level * 0.05;
    float al = 1.0 - smoothstep(0.985, 1.0, sqrt(r2));
    gl_FragColor = vec4(col * al, al);
}
"""

private class Gl(private val st: SurfaceTexture, private var w: Int, private var h: Int) {
    private var dpy: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var ctx: EGLContext = EGL14.EGL_NO_CONTEXT
    private var surf: EGLSurface = EGL14.EGL_NO_SURFACE
    private var prog = 0
    private var uRes = 0
    private var uOrg = 0
    private var uRad = 0
    private var uT = 0
    private var uLevel = 0
    private var uDim = 0
    private var aPos = 0
    private val quad: FloatBuffer = ByteBuffer.allocateDirect(32).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
        put(floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f))
        position(0)
    }
    private var lt = Float.NaN
    private var ll = Float.NaN
    private var ld = Float.NaN

    fun init() {
        dpy = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        check(dpy != EGL14.EGL_NO_DISPLAY) { "no display" }
        val v = IntArray(2)
        check(EGL14.eglInitialize(dpy, v, 0, v, 1)) { "eglInitialize" }
        val attr = intArrayOf(
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_NONE,
        )
        val cfgs = arrayOfNulls<EGLConfig>(1)
        val n = IntArray(1)
        check(EGL14.eglChooseConfig(dpy, attr, 0, cfgs, 0, 1, n, 0) && n[0] > 0) { "eglChooseConfig" }
        ctx = EGL14.eglCreateContext(dpy, cfgs[0], EGL14.EGL_NO_CONTEXT, intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE), 0)
        check(ctx != EGL14.EGL_NO_CONTEXT) { "eglCreateContext" }
        surf = EGL14.eglCreateWindowSurface(dpy, cfgs[0], st, intArrayOf(EGL14.EGL_NONE), 0)
        check(surf != EGL14.EGL_NO_SURFACE) { "eglCreateWindowSurface" }
        check(EGL14.eglMakeCurrent(dpy, surf, surf, ctx)) { "eglMakeCurrent" }
        val vs = shader(GLES20.GL_VERTEX_SHADER, VS)
        val fs = shader(GLES20.GL_FRAGMENT_SHADER, FS)
        prog = GLES20.glCreateProgram()
        GLES20.glAttachShader(prog, vs)
        GLES20.glAttachShader(prog, fs)
        GLES20.glLinkProgram(prog)
        val ok = IntArray(1)
        GLES20.glGetProgramiv(prog, GLES20.GL_LINK_STATUS, ok, 0)
        check(ok[0] != 0) { "link: " + GLES20.glGetProgramInfoLog(prog) }
        GLES20.glDeleteShader(vs)
        GLES20.glDeleteShader(fs)
        GLES20.glUseProgram(prog)
        aPos = GLES20.glGetAttribLocation(prog, "pos")
        uRes = GLES20.glGetUniformLocation(prog, "res")
        uOrg = GLES20.glGetUniformLocation(prog, "org")
        uRad = GLES20.glGetUniformLocation(prog, "rad")
        uT = GLES20.glGetUniformLocation(prog, "t")
        uLevel = GLES20.glGetUniformLocation(prog, "level")
        uDim = GLES20.glGetUniformLocation(prog, "dim")
        GLES20.glUniform3f(GLES20.glGetUniformLocation(prog, "c0"), 0.890f, 0.604f, 0.239f)
        GLES20.glUniform3f(GLES20.glGetUniformLocation(prog, "c1"), 0.984f, 0.890f, 0.710f)
        GLES20.glUniform3f(GLES20.glGetUniformLocation(prog, "c2"), 1.000f, 0.973f, 0.918f)
        GLES20.glUniform3f(GLES20.glGetUniformLocation(prog, "c3"), 0.949f, 0.714f, 0.380f)
        GLES20.glUniform3f(GLES20.glGetUniformLocation(prog, "c4"), 1.000f, 0.922f, 0.784f)
        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glClearColor(0f, 0f, 0f, 0f)
    }

    private fun shader(type: Int, src: String): Int {
        val s = GLES20.glCreateShader(type)
        GLES20.glShaderSource(s, src)
        GLES20.glCompileShader(s)
        val ok = IntArray(1)
        GLES20.glGetShaderiv(s, GLES20.GL_COMPILE_STATUS, ok, 0)
        check(ok[0] != 0) { "shader: " + GLES20.glGetShaderInfoLog(s) }
        return s
    }

    fun resize(nw: Int, nh: Int) {
        w = nw
        h = nh
        lt = Float.NaN
    }

    fun draw(t: Float, level: Float, dim: Float) {
        if (t == lt && level == ll && dim == ld) return
        lt = t
        ll = level
        ld = dim
        val m = minOf(w, h).toFloat()
        GLES20.glViewport(0, 0, w, h)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUniform2f(uRes, w.toFloat(), h.toFloat())
        GLES20.glUniform2f(uOrg, w / 2f, h / 2f - m * 0.06f)
        GLES20.glUniform1f(uRad, m * 0.34f)
        GLES20.glUniform1f(uT, t)
        GLES20.glUniform1f(uLevel, level)
        GLES20.glUniform1f(uDim, dim)
        GLES20.glEnableVertexAttribArray(aPos)
        GLES20.glVertexAttribPointer(aPos, 2, GLES20.GL_FLOAT, false, 8, quad)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        check(EGL14.eglSwapBuffers(dpy, surf)) { "eglSwapBuffers" }
    }

    fun release() {
        if (dpy != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(dpy, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            if (surf != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(dpy, surf)
            if (ctx != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(dpy, ctx)
            EGL14.eglTerminate(dpy)
        }
        dpy = EGL14.EGL_NO_DISPLAY
        ctx = EGL14.EGL_NO_CONTEXT
        surf = EGL14.EGL_NO_SURFACE
    }
}

class OrbGlView(context: Context, private val onFail: (Throwable) -> Unit) :
    TextureView(context), TextureView.SurfaceTextureListener, LifecycleEventObserver {

    @Volatile var phase: () -> Float = { 0f }
    @Volatile var level: () -> Float = { 0f }
    @Volatile var dim: () -> Float = { 0f }

    private val main = Handler(Looper.getMainLooper())
    private var thread: HandlerThread? = null
    private var h: Handler? = null
    private var gl: Gl? = null
    private var choreo: Choreographer? = null
    private var owner: LifecycleOwner? = null
    @Volatile private var started = true
    @Volatile private var ready = false
    @Volatile private var failed = false

    private val frame = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            val g = gl ?: return
            if (!ready || !started) return
            try {
                g.draw(phase(), level(), dim())
            } catch (e: Throwable) {
                fail(e)
                return
            }
            choreo?.postFrameCallback(this)
        }
    }

    init {
        isOpaque = false
        surfaceTextureListener = this
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    fun bind(o: LifecycleOwner) {
        owner?.lifecycle?.removeObserver(this)
        owner = o
        started = o.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        o.lifecycle.addObserver(this)
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_START -> started = true
            Lifecycle.Event.ON_STOP -> started = false
            else -> return
        }
        sync()
    }

    private fun sync() {
        h?.post {
            val c = choreo ?: return@post
            c.removeFrameCallback(frame)
            if (ready && started && gl != null) c.postFrameCallback(frame)
        }
    }

    private fun fail(e: Throwable) {
        if (failed) return
        failed = true
        ready = false
        main.post { onFail(e) }
    }

    override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, hh: Int) {
        val t = HandlerThread("orb-gl").also { it.start() }
        thread = t
        val hd = Handler(t.looper)
        h = hd
        hd.post {
            try {
                choreo = Choreographer.getInstance()
                gl = Gl(st, w, hh).also { it.init() }
                ready = true
                if (started) choreo?.postFrameCallback(frame)
            } catch (e: Throwable) {
                gl?.release()
                gl = null
                fail(e)
            }
        }
    }

    override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, hh: Int) {
        h?.post { gl?.resize(w, hh) }
    }

    override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
        ready = false
        val t = thread
        h?.post {
            choreo?.removeFrameCallback(frame)
            gl?.release()
            gl = null
        }
        t?.quitSafely()
        try {
            t?.join(500)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        thread = null
        h = null
        choreo = null
        return true
    }

    override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}

    override fun onDetachedFromWindow() {
        owner?.lifecycle?.removeObserver(this)
        super.onDetachedFromWindow()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        owner?.let {
            it.lifecycle.addObserver(this)
            started = it.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
            sync()
        }
    }
}
