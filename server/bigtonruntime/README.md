
# Bigton C runtime

To generate Java glue headers, run the following command in the `jni`-directory:
```bash
javac -h ../glue/jni -d ../java-out schwalbe/ventura/bigton/runtime/*.java
```
This will generate several headers in `glue/jni` for the glue code in `glue` to use.

## Compilation

To compile this into a dynamic library loadable by the JVM:
- Compile all `.c` files in the `glue` and `src` directories
- Link the maths library: `-lm`
- Include BIGTON headers: `-I $(realpath include)`
- Include JNI headers: `-I $(realpath $(dirname $(readlink -f /usr/bin/javac))/../include/)`
- Include Linux JNI headers (if on Linux): `-I $(realpath $(dirname $(readlink -f /usr/bin/javac))/../include/linux/)`

## IDE Headers

As seen in the previous section, the glue code relies on JNI headers provided by your JVM implementation. On a system using a POSIX shell, you can get the include paths using:
```
echo $(realpath include)
echo $(realpath $(dirname $(readlink -f /usr/bin/javac))/../include/)
```
Linux systems additionally need to include the Linux JNI headers:
```
echo $(realpath $(dirname $(readlink -f /usr/bin/javac))/../include/linux/)
```

Add these paths as include paths to your IDE language server. For example, Zed uses Clangd, so users of Zed can add a `.clangd` file in this directory:
```yaml
CompileFlags:
  Add:
    # Run the commands shown above to get your path(s)
    - "-I/home/schwalbe/Projects/kotlin/ventura/server/bigtonruntime/include"
    - "-I/usr/lib/jvm/java-21-openjdk/include/"
    - "-I/usr/lib/jvm/java-21-openjdk/include/linux/"
```