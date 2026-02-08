
#pragma once

vec3 clipToNdc(vec4 p) { return p.xyz / p.w; }

vec3 ndcToNorm(vec3 ndc) { return ndc * 0.5 + 0.5; }