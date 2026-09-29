#include <jni.h>
#include <algorithm>
#include <cerrno>
#include <cstdint>
#include <cstring>
#include <cstdio>
#include <fcntl.h>
#include <string>
#include <cstdlib>
#include <sys/ioctl.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <sys/syscall.h>
#include <unistd.h>
#include <sys/wait.h>

static std::string read_fd(int fd) {
    if (fd < 0) return "fd=-1 errno=" + std::to_string(errno);
    char b[4096];
    ssize_t n = read(fd, b, sizeof(b) - 1);
    if (n < 0) return "read=-1 errno=" + std::to_string(errno);
    b[n] = 0;
    return "read=" + std::to_string(n) + "\n" + std::string(b, n);
}

static std::string libc_openat(const char* p) {
    int fd = openat(AT_FDCWD, p, O_RDONLY | O_CLOEXEC);
    auto s = read_fd(fd);
    if (fd >= 0) close(fd);
    return "LIBC openat:\n" + s;
}

static std::string libc_open(const char* p) {
    int fd = open(p, O_RDONLY | O_CLOEXEC);
    auto s = read_fd(fd);
    if (fd >= 0) close(fd);
    return "LIBC open:\n" + s;
}

static std::string direct_openat(const char* p) {
#ifdef SYS_openat
    int fd = (int)syscall(SYS_openat, AT_FDCWD, p, O_RDONLY | O_CLOEXEC, 0);
    auto s = read_fd(fd);
    if (fd >= 0) close(fd);
    return "DIRECT syscall openat:\n" + s;
#else
    return "DIRECT syscall openat: unavailable";
#endif
}

static std::string direct_openat2(const char* p) {
#ifdef SYS_openat2
    struct open_how_test {
        uint64_t flags;
        uint64_t mode;
        uint64_t resolve;
    };
    open_how_test h{(uint64_t)(O_RDONLY | O_CLOEXEC), 0, 0};
    int fd = (int)syscall(SYS_openat2, AT_FDCWD, p, &h, sizeof(h));
    auto s = read_fd(fd);
    if (fd >= 0) close(fd);
    return "DIRECT syscall openat2:\n" + s;
#else
    return "DIRECT syscall openat2: unavailable";
#endif
}

static std::string libc_fopen(const char* p) {
    FILE* f = fopen(p, "rb");
    if (!f) return "LIBC fopen:\nerrno=" + std::to_string(errno);
    char b[4096];
    size_t n = fread(b, 1, sizeof(b) - 1, f);
    fclose(f);
    b[n] = 0;
    return "LIBC fopen:\nread=" + std::to_string(n) + "\n" + std::string(b, n);
}

static std::string pread_test(const char* p) {
    int fd = openat(AT_FDCWD, p, O_RDONLY | O_CLOEXEC);
    if (fd < 0) return "pread: open errno=" + std::to_string(errno);
    char b[4096];
    ssize_t n = pread(fd, b, sizeof(b) - 1, 0);
    int e = errno;
    if (n > 0) b[n] = 0;
    close(fd);
    return "pread after openat:\nread=" + std::to_string(n) +
           (n < 0 ? "\nerrno=" + std::to_string(e) : "\n" + std::string(b, n));
}

static std::string mmap_test(const char* p) {
    int fd = openat(AT_FDCWD, p, O_RDONLY | O_CLOEXEC);
    if (fd < 0) return "mmap: open errno=" + std::to_string(errno);

    struct stat st{};
    if (fstat(fd, &st) != 0) {
        int e = errno;
        close(fd);
        return "mmap: fstat errno=" + std::to_string(e);
    }
    if (st.st_size <= 0) {
        close(fd);
        return "mmap: size=" + std::to_string((long long)st.st_size);
    }

    size_t len = (size_t)st.st_size;
    void* m = mmap(nullptr, len, PROT_READ, MAP_PRIVATE, fd, 0);
    if (m == MAP_FAILED) {
        int e = errno;
        close(fd);
        return "mmap: failed errno=" + std::to_string(e);
    }

    size_t n = std::min(len, (size_t)4096);
    std::string s = "mmap: OK size=" + std::to_string(len) + "\n" +
                    std::string((const char*)m, n);
    munmap(m, len);
    close(fd);
    return s;
}

static std::string capture_execve(const char* p) {
    int pipefd[2];
    if (pipe(pipefd) != 0) return "execve: pipe errno=" + std::to_string(errno);
    pid_t pid = fork();
    if (pid < 0) {
        int e = errno;
        close(pipefd[0]);
        close(pipefd[1]);
        return "execve: fork errno=" + std::to_string(e);
    }
    if (pid == 0) {
        dup2(pipefd[1], STDOUT_FILENO);
        dup2(pipefd[1], STDERR_FILENO);
        close(pipefd[0]);
        close(pipefd[1]);
        char* const argv[] = {(char*)"cat", (char*)p, nullptr};
        char* const envp[] = {(char*)"PATH=/system/bin:/system/xbin", nullptr};
        execve("/system/bin/cat", argv, envp);
        _exit(127);
    }
    close(pipefd[1]);
    char b[4096];
    std::string out;
    ssize_t n;
    while ((n = read(pipefd[0], b, sizeof(b))) > 0) out.append(b, n);
    close(pipefd[0]);
    int st = 0;
    waitpid(pid, &st, 0);
    int code = WIFEXITED(st) ? WEXITSTATUS(st) : -1;
    return "EXECVE /system/bin/cat: exit=" + std::to_string(code) + "\n" + out;
}

static std::string capture_system_cat(const char* p) {
    int pipefd[2];
    if (pipe(pipefd) != 0) return "system: pipe errno=" + std::to_string(errno);
    pid_t pid = fork();
    if (pid < 0) {
        int e = errno;
        close(pipefd[0]);
        close(pipefd[1]);
        return "system: fork errno=" + std::to_string(e);
    }
    if (pid == 0) {
        dup2(pipefd[1], STDOUT_FILENO);
        dup2(pipefd[1], STDERR_FILENO);
        close(pipefd[0]);
        close(pipefd[1]);
        std::string cmd = "cat '" + std::string(p) + "'";
        int r = system(cmd.c_str());
        _exit(r == -1 ? 127 : (WIFEXITED(r) ? WEXITSTATUS(r) : 128));
    }
    close(pipefd[1]);
    char b[4096];
    std::string out;
    ssize_t n;
    while ((n = read(pipefd[0], b, sizeof(b))) > 0) out.append(b, n);
    close(pipefd[0]);
    int st = 0;
    waitpid(pid, &st, 0);
    int code = WIFEXITED(st) ? WEXITSTATUS(st) : -1;
    return "SYSTEM cat: exit=" + std::to_string(code) + "\n" + out;
}

static std::string ioctl_test(const char* p) {
    int fd = openat(AT_FDCWD, p, O_RDONLY | O_CLOEXEC);
    if (fd < 0) return "ioctl: open errno=" + std::to_string(errno);
    int v = 0;
    int r = ioctl(fd, FIONREAD, &v);
    int e = errno;
    close(fd);
    return "ioctl(FIONREAD): ret=" + std::to_string(r) +
           " value=" + std::to_string(v) +
           (r < 0 ? " errno=" + std::to_string(e) : "");
}

static std::string runOne(const char* p, const char* mode) {
    if (strcmp(mode, "libc openat") == 0) return libc_openat(p);
    if (strcmp(mode, "libc open") == 0) return libc_open(p);
    if (strcmp(mode, "direct syscall openat") == 0) return direct_openat(p);
    if (strcmp(mode, "direct syscall openat2") == 0) return direct_openat2(p);
    if (strcmp(mode, "fopen") == 0) return libc_fopen(p);
    if (strcmp(mode, "pread") == 0) return pread_test(p);
    if (strcmp(mode, "mmap") == 0) return mmap_test(p);
    if (strcmp(mode, "ioctl") == 0) return ioctl_test(p);
    if (strcmp(mode, "system cat") == 0) return capture_system_cat(p);
    if (strcmp(mode, "execve cat") == 0) return capture_execve(p);
    return "Unknown test: " + std::string(mode);
}

static std::string native_cmd(const char* c) {
    FILE* f = popen(c, "r");
    if (!f) return std::string(c) + " [popen errno=" + std::to_string(errno) + "]";
    char b[4096];
    std::string out;
    size_t n;
    while ((n = fread(b, 1, sizeof(b), f)) > 0) out.append(b, n);
    int r = pclose(f);
    return std::string(c) + " [status=" + std::to_string(r) + "]\n" + out;
}

static std::string root_test() {
    std::string out = "=== ROOT / UID (NATIVE) ===\n";
    out += native_cmd("which su");
    out += "\n";
    out += native_cmd("id");
    out += "\n";
    out += native_cmd("id -u");
    out += "\nPROCESS_UID=" + std::to_string(getuid());
    out += "\nPROCESS_GID=" + std::to_string(getgid());
    out += "\n";
    out += native_cmd("su -c id");
    return out;
}

static std::string runMaps(const char* p) {
    const char* modes[] = {
        "libc openat", "libc open",
        "direct syscall openat", "fopen", "pread",
        "system cat", "execve cat"
    };
    std::string out = "PID=" + std::to_string(getpid()) + "\nPATH=" + p + "\n\n";
    for (const char* m : modes) {
        out += "=== " + std::string(m) + " ===\n";
        out += runOne(p, m);
        out += "\n\n";
    }
    return out;
}

static std::string runAll(const char* p) {
    const char* modes[] = {
        "libc openat", "libc open", "direct syscall openat",
        "direct syscall openat2", "fopen", "pread", "mmap", "ioctl",
        "system cat", "execve cat"
    };
    std::string out = "PID=" + std::to_string(getpid()) + "\nPATH=" + p + "\n\n";
    for (const char* m : modes) {
        out += "=== " + std::string(m) + " ===\n";
        out += runOne(p, m);
        out += "\n\n";
    }
    return out;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_erfansst_libtester_MainActivity_nativeTest(
        JNIEnv* env, jobject, jstring jp, jstring jm) {
    if (!jp || !jm) return env->NewStringUTF("JNI: null argument");
    const char* p = env->GetStringUTFChars(jp, nullptr);
    const char* m = env->GetStringUTFChars(jm, nullptr);
    if (!p || !m) {
        if (p) env->ReleaseStringUTFChars(jp, p);
        if (m) env->ReleaseStringUTFChars(jm, m);
        return env->NewStringUTF("JNI: GetStringUTFChars failed");
    }

    std::string s;
    if (strcmp(m, "all") == 0) s = runAll(p);
    else if (strcmp(m, "maps") == 0) s = runMaps(p);
    else if (strcmp(m, "root") == 0) s = root_test();
    else s = runOne(p, m);

    env->ReleaseStringUTFChars(jp, p);
    env->ReleaseStringUTFChars(jm, m);
    return env->NewStringUTF(s.c_str());
}
