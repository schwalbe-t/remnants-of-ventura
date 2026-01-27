
#pragma once

#define MAX_NUM_INSTANCES 1000
#define MAX_NUM_JOINTS 64

layout(std140) uniform uInstances {
    mat4 instances[MAX_NUM_INSTANCES];
};

uniform mat4 uLocalTransform;
uniform mat4 uJointTransforms[MAX_NUM_JOINTS];
uniform mat4 uViewProjection;
