
# Bigton C runtime

- `src/main/c/jni/generated/` - Machine-generated C JNI headers
- `src/main/c/jni/` - Glue code implementing JNI interface functions
- `src/main/c/runtime/` - Implementation of the BIGTON runtime in C
- `src/main/headers/bigton/` - BIGTON runtime API C headers
- `src/main/kotlin/.../` - High-level Kotlin wrapper API

### Standalone Executable

The BIGTON runtime can be compiled into a bare-bones standalone executable
for testing purposes. To do this, run the following command:
```bash
cc src/main/c/runtime/*.c -I src/main/headers -lm -O3 -g -o bigton
```
This generates a `bigton` executable than can be used by running
```bash
./bigton <file>
```

### IDEs

As seen in the previous section, the glue code relies on JNI headers provided by your JVM implementation. On a system using a POSIX shell, you can get the include paths using:
```
echo $(realpath src/main/headers)
echo $(realpath $(dirname $(readlink -f /usr/bin/javac))/../include/)
```
Additionally you will also need to include system-specific JNI headers
(`<SYSTEM>` = `darwin` on MacOS, `linux` on Linux, `win32` on Windows):
```
echo $(realpath $(dirname $(readlink -f /usr/bin/javac))/../include/<SYSTEM>/)
```

Add these paths as include paths to your IDE language server. For example, Zed uses Clangd, so users of Zed can add a `.clangd` file in this directory:
```yaml
CompileFlags:
  Add:
    # Run the commands shown above to get your path(s)
    - "-I/home/schwalbe/Projects/kotlin/ventura/server/bigtonruntime/src/main/headers"
    - "-I/usr/lib/jvm/java-21-openjdk/include/"
    - "-I/usr/lib/jvm/java-21-openjdk/include/linux/"
```