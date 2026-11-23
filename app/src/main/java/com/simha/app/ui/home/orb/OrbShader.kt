package com.simha.app.ui.home.orb

import android.graphics.RuntimeShader
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.ShaderBrush

private const val SRC = """
uniform float2 org;
uniform float rad;
uniform float t;
uniform float level;
uniform float dim;
uniform float3 c0;
uniform float3 c1;
uniform float3 c2;
uniform float3 c3;
uniform float3 c4;

float hs(float3 p) {
    p = fract(p * 0.3183099 + 0.1);
    p *= 17.0;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}

float vn(float3 x) {
    float3 i = floor(x);
    float3 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    float a = mix(mix(hs(i), hs(i + float3(1.0, 0.0, 0.0)), f.x),
                  mix(hs(i + float3(0.0, 1.0, 0.0)), hs(i + float3(1.0, 1.0, 0.0)), f.x), f.y);
    float b = mix(mix(hs(i + float3(0.0, 0.0, 1.0)), hs(i + float3(1.0, 0.0, 1.0)), f.x),
                  mix(hs(i + float3(0.0, 1.0, 1.0)), hs(i + float3(1.0, 1.0, 1.0)), f.x), f.y);
    return mix(a, b, f.z);
}

float fbm(float3 x) {
    float a = 0.5;
    float s = 0.0;
    for (int i = 0; i < 4; i++) {
        s += a * vn(x);
        x = x * 2.02 + float3(1.7, 9.2, 3.1);
        a *= 0.5;
    }
    return s;
}

half4 main(float2 fc) {
    float2 p = (fc - org) / rad;
    float r2 = dot(p, p);
    if (r2 > 1.0) {
        return half4(0.0);
    }
    float z = sqrt(1.0 - r2);
    float3 n = float3(p.x, p.y, z);
    float a = t * 0.35;
    float3 q = n * 1.3;
    q = float3(q.x * cos(a) + q.z * sin(a), q.y, -q.x * sin(a) + q.z * cos(a));
    float amp = 1.0 + level * 1.2;
    float3 w = float3(
        fbm(q + float3(0.0, 0.0, t * 0.2)),
        fbm(q + float3(5.2, 1.3, -t * 0.17)),
        fbm(q + float3(2.4, 7.1, t * 0.13))
    );
    float f = fbm(q + (w - 0.5) * 3.2 * amp + float3(0.0, t * 0.1, 0.0));
    float f2 = fbm(q * 1.5 - w * 1.5 + 3.0);
    float3 col = mix(c1, c2, smoothstep(0.25, 0.75, f));
    col = mix(col, c0, (1.0 - smoothstep(0.30, 0.58, f2)) * 0.8);
    col = mix(col, c3, smoothstep(0.55, 0.85, w.x) * 0.55);
    col = mix(col, c4, smoothstep(0.60, 0.90, w.y) * 0.7);
    float3 l = normalize(float3(-0.5, -0.6, 0.65));
    float diff = clamp(dot(n, l), 0.0, 1.0);
    col *= 0.62 + 0.5 * diff;
    float rim = pow(1.0 - z, 2.2);
    float2 dir = p / (sqrt(r2) + 0.0001);
    col *= 1.0 - rim * (0.25 + 0.45 * max(dot(dir, float2(0.7071, 0.7071)), 0.0));
    float3 h = normalize(l + float3(0.0, 0.0, 1.0));
    float nh = max(dot(n, h), 0.0);
    col += c4 * (pow(nh, 60.0) * 0.7 + pow(nh, 8.0) * 0.12);
    col *= 1.0 - 0.18 * dim;
    col += level * 0.05;
    float al = 1.0 - smoothstep(0.985, 1.0, sqrt(r2));
    return half4(half3(col * al), half(al));
}
"""

@RequiresApi(33)
class OrbShader {
    private val s = RuntimeShader(SRC)
    val brush = ShaderBrush(s)

    init {
        s.setFloatUniform("c0", 0.494f, 0.184f, 0.231f)
        s.setFloatUniform("c1", 0.914f, 0.635f, 0.675f)
        s.setFloatUniform("c2", 0.957f, 0.780f, 0.690f)
        s.setFloatUniform("c3", 0.886f, 0.698f, 0.416f)
        s.setFloatUniform("c4", 1.000f, 0.945f, 0.886f)
    }

    fun set(ox: Float, oy: Float, rad: Float, t: Float, level: Float, dim: Float) {
        s.setFloatUniform("org", ox, oy)
        s.setFloatUniform("rad", rad)
        s.setFloatUniform("t", t)
        s.setFloatUniform("level", level)
        s.setFloatUniform("dim", dim)
    }
}
