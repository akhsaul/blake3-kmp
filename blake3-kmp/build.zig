const std = @import("std");
const builtin = @import("builtin");

pub fn build(b: *std.Build) void {
    const ndk_opt = b.option([]const u8, "ndk", "Path to Android NDK");
    const jvm_step = b.step("jvm", "Build JVM Desktop targets");
    const android_step = b.step("android", "Build Android targets");

    // --- Desktop Targets ---
    setupTarget(b, jvm_step, .linux, .aarch64, .gnu, "aarch64", null);
    setupTarget(b, jvm_step, .linux, .x86_64, .gnu, "amd64", null);
    setupTarget(b, jvm_step, .macos, .aarch64, null, "aarch64", null);
    setupTarget(b, jvm_step, .macos, .x86_64, null, "x86_64", null);
    setupTarget(b, jvm_step, .windows, .x86_64, null, "amd64", null);
    setupTarget(b, jvm_step, .windows, .aarch64, null, "aarch64", null);

    // --- Android Targets ---
    const ndk_path = getNdkPath(b, ndk_opt);
    setupTarget(b, android_step, .linux, .aarch64, .android, "arm64-v8a", ndk_path);
    setupTarget(b, android_step, .linux, .arm, .androideabi, "armeabi-v7a", ndk_path);
    setupTarget(b, android_step, .linux, .x86_64, .android, "x86_64", ndk_path);
    setupTarget(b, android_step, .linux, .x86, .android, "x86", ndk_path);
}

fn setupTarget(
    b: *std.Build,
    group_step: *std.Build.Step,
    tag: std.Target.Os.Tag,
    arch: std.Target.Cpu.Arch,
    abi: ?std.Target.Abi,
    dir: []const u8,
    ndk_path: ?[]const u8,
) void {
    const mod = b.createModule(.{
        .target = b.resolveTargetQuery(.{
            .cpu_arch = arch,
            .os_tag = tag,
            .abi = abi,
        }),
        .optimize = .ReleaseFast,
    });

    mod.addIncludePath(b.path("native/include/share"));
    mod.addIncludePath(
        switch (tag) {
            .windows => b.path("native/include/windows"),
            else => b.path("native/include/unix"),
        },
    );
    mod.addIncludePath(b.path("../BLAKE3/c"));

    const is_android = if (abi) |a| (a == .android or a == .androideabi) else false;
    if (is_android) {
        addNdkSysroot(b, mod, arch, ndk_path);
    } else {
        mod.link_libc = true;
    }
    if (tag == .macos or tag == .ios) {
        mod.linkSystemLibrary("System", .{});
    }

    mod.addCSourceFiles(.{
        .files = &.{
            "../BLAKE3/c/blake3.c",
            "../BLAKE3/c/blake3_dispatch.c",
            "../BLAKE3/c/blake3_portable.c",
            "native/Blake3Kmp.c",
        },
        .flags = &.{
            "-std=c99",
        },
    });

    if (arch == .x86_64) {
        if (tag == .windows) {
            mod.addCSourceFiles(.{
                .files = &.{
                    "../BLAKE3/c/blake3_sse2_x86-64_windows_gnu.S",
                    "../BLAKE3/c/blake3_sse41_x86-64_windows_gnu.S",
                    "../BLAKE3/c/blake3_avx2_x86-64_windows_gnu.S",
                    "../BLAKE3/c/blake3_avx512_x86-64_windows_gnu.S",
                },
                .flags = &.{},
            });
        } else {
            mod.addCSourceFiles(.{
                .files = &.{
                    "../BLAKE3/c/blake3_sse2_x86-64_unix.S",
                    "../BLAKE3/c/blake3_sse41_x86-64_unix.S",
                    "../BLAKE3/c/blake3_avx2_x86-64_unix.S",
                    "../BLAKE3/c/blake3_avx512_x86-64_unix.S",
                },
                .flags = &.{},
            });
        }
    } else if (arch == .x86) {
        mod.addCMacro("BLAKE3_NO_SSE2", "1");
        mod.addCMacro("BLAKE3_NO_SSE41", "1");
        mod.addCMacro("BLAKE3_NO_AVX2", "1");
        mod.addCMacro("BLAKE3_NO_AVX512", "1");
    } else if (arch == .aarch64) {
        mod.addCSourceFiles(.{
            .files = &.{
                "../BLAKE3/c/blake3_neon.c",
            },
            .flags = &.{
                "-std=c99",
                "-DBLAKE3_USE_NEON=1",
            },
        });
    }

    const lib = b.addLibrary(.{
        .name = "blake3-kmp",
        .root_module = mod,
        .linkage = .dynamic,
    });

    const install = b.addInstallArtifact(lib, .{
        .dest_dir = .{
            .override = .{
                .custom = dir,
            },
        },
    });

    group_step.dependOn(&install.step);
}

fn getNdkPath(b: *std.Build, ndk_opt: ?[]const u8) ?[]const u8 {
    if (ndk_opt) |p| if (p.len > 0) return p;
    if (b.graph.environ_map.get("ANDROID_NDK_HOME")) |p| if (p.len > 0) return p;
    if (b.graph.environ_map.get("ANDROID_NDK_ROOT")) |p| if (p.len > 0) return p;
    if (b.graph.environ_map.get("ANDROID_NDK_LATEST_HOME")) |p| if (p.len > 0) return p;
    if (b.graph.environ_map.get("ANDROID_NDK")) |p| if (p.len > 0) return p;
    if (b.graph.environ_map.get("ANDROID_HOME")) |p| {
        if (p.len > 0) return b.fmt("{s}/ndk-bundle", .{p});
    }
    return "/usr/local/lib/android/sdk/ndk-bundle";
}

fn addNdkSysroot(b: *std.Build, mod: *std.Build.Module, arch: std.Target.Cpu.Arch, ndk_path: ?[]const u8) void {
    const ndk = ndk_path orelse return;
    const host_tag = switch (builtin.os.tag) {
        .macos => "darwin-x86_64",
        .windows => "windows-x86_64",
        else => "linux-x86_64",
    };
    const triple_str = switch (arch) {
        .aarch64 => "aarch64-linux-android",
        .arm => "arm-linux-androideabi",
        .x86_64 => "x86_64-linux-android",
        .x86 => "i686-linux-android",
        else => "aarch64-linux-android",
    };

    // Modern LLVM sysroot layout
    const sysroot = b.fmt("{s}/toolchains/llvm/prebuilt/{s}/sysroot", .{ ndk, host_tag });
    mod.addSystemIncludePath(.{ .cwd_relative = b.fmt("{s}/usr/include", .{sysroot}) });
    mod.addSystemIncludePath(.{ .cwd_relative = b.fmt("{s}/usr/include/{s}", .{ sysroot, triple_str }) });

    // NDK Library paths for dynamic linker
    mod.addLibraryPath(.{ .cwd_relative = b.fmt("{s}/usr/lib/{s}/24", .{ sysroot, triple_str }) });
    mod.addLibraryPath(.{ .cwd_relative = b.fmt("{s}/usr/lib/{s}", .{ sysroot, triple_str }) });

    // Link NDK libc.so and libm.so directly so LLD records DT_NEEDED
    mod.addObjectFile(.{ .cwd_relative = b.fmt("{s}/usr/lib/{s}/24/libc.so", .{ sysroot, triple_str }) });
    mod.addObjectFile(.{ .cwd_relative = b.fmt("{s}/usr/lib/{s}/24/libm.so", .{ sysroot, triple_str }) });

    // Direct sysroot layout fallback
    mod.addSystemIncludePath(.{ .cwd_relative = b.fmt("{s}/sysroot/usr/include", .{ndk}) });
    mod.addSystemIncludePath(.{ .cwd_relative = b.fmt("{s}/sysroot/usr/include/{s}", .{ ndk, triple_str }) });
}
