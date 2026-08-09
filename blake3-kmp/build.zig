const std = @import("std");

pub fn build(b: *std.Build) !void {
    const deleteLib = b.addRemoveDirTree(.{ .cwd_relative = b.getInstallPath(.prefix, "lib") });
    b.getInstallStep().dependOn(&deleteLib.step);

    // --- Desktop Targets ---
    try setupTarget(b, &deleteLib.step, .linux, .aarch64, .gnu, "aarch64");
    try setupTarget(b, &deleteLib.step, .linux, .x86_64, .gnu, "amd64");
    try setupTarget(b, &deleteLib.step, .macos, .aarch64, null, "aarch64");
    try setupTarget(b, &deleteLib.step, .macos, .x86_64, null, "x86_64");
    try setupTarget(b, &deleteLib.step, .windows, .x86_64, null, "amd64");
    try setupTarget(b, &deleteLib.step, .windows, .aarch64, null, "aarch64");
}

fn setupTarget(
    b: *std.Build,
    step: *std.Build.Step,
    tag: std.Target.Os.Tag,
    arch: std.Target.Cpu.Arch,
    abi: ?std.Target.Abi,
    dir: []const u8,
) !void {
    const lib = b.addLibrary(.{
        .name = "blake3-kmp",
        .root_module = b.createModule(.{
            .target = b.resolveTargetQuery(.{
                .cpu_arch = arch,
                .os_tag = tag,
                .abi = abi,
            }),
            .optimize = .ReleaseFast,
        }),
        .linkage = .dynamic,
    });

    lib.addIncludePath(b.path("native/include/share"));
    lib.addIncludePath(
        switch (tag) {
            .windows => b.path("native/include/windows"),
            else => b.path("native/include/unix"),
        },
    );
    lib.addIncludePath(b.path("../BLAKE3/c"));


    lib.linkLibC();
    if (tag == .macos or tag == .ios) {
        lib.linkSystemLibrary("System");
    }


    lib.addCSourceFiles(.{
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
            lib.addCSourceFiles(.{
                .files = &.{
                    "../BLAKE3/c/blake3_sse2_x86-64_windows_gnu.S",
                    "../BLAKE3/c/blake3_sse41_x86-64_windows_gnu.S",
                    "../BLAKE3/c/blake3_avx2_x86-64_windows_gnu.S",
                    "../BLAKE3/c/blake3_avx512_x86-64_windows_gnu.S",
                },
                .flags = &.{},
            });
        } else {
            lib.addCSourceFiles(.{
                .files = &.{
                    "../BLAKE3/c/blake3_sse2_x86-64_unix.S",
                    "../BLAKE3/c/blake3_sse41_x86-64_unix.S",
                    "../BLAKE3/c/blake3_avx2_x86-64_unix.S",
                    "../BLAKE3/c/blake3_avx512_x86-64_unix.S",
                },
                .flags = &.{},
            });
        }
    } else if (arch == .aarch64 or arch == .arm) {
        lib.addCSourceFiles(.{
            .files = &.{
                "../BLAKE3/c/blake3_neon.c",
            },
            .flags = &.{
                "-std=c99",
                "-DBLAKE3_USE_NEON=1",
            },
        });
    }

    const install = b.addInstallArtifact(lib, .{
        .dest_dir = .{
            .override = .{
                .custom = dir,
            },
        },
    });

    step.dependOn(&install.step);
}
