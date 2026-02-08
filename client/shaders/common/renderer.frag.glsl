
#pragma once

uniform sampler2D uTexture;

uniform vec3 uBaseFactor;
uniform vec3 uShadowFactor;
uniform vec3 uOutlineFactor;

uniform vec3 uGroundToSun;
uniform mat4 uSunViewProjection;
uniform sampler2DMS uShadowMap;
uniform int uShadowMapSamples;
uniform float uDepthBias;
uniform float uNormalOffset;
uniform bool uDefaultLit;
