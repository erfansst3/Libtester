#include <jni.h>
#include <cerrno>
#include <cstdint>
#include <cstdio>
#include <cstring>
#include <fcntl.h>
#include <string>
#include <sys/ioctl.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <sys/syscall.h>
#include <unistd.h>

static std::string read_fd(int fd){if(fd<0)return "fd=-1 errno="+std::to_string(errno);char b[4096];ssize_t n=read(fd,b,sizeof(b)-1);if(n<0)return "read=-1 errno="+std::to_string(errno);b[n]=0;return "read="+std::to_string(n)+"\n"+std::string(b,n);}
static std::string libc_openat(const char*p){int fd=openat(AT_FDCWD,p,O_RDONLY|O_CLOEXEC);auto s=read_fd(fd);if(fd>=0)close(fd);return "LIBC openat:\n"+s;}
static std::string libc_open(const char*p){int fd=open(p,O_RDONLY|O_CLOEXEC);auto s=read_fd(fd);if(fd>=0)close(fd);return "LIBC open:\n"+s;}
static std::string direct_openat(const char*p){
#ifdef SYS_openat
int fd=(int)syscall(SYS_openat,AT_FDCWD,p,O_RDONLY|O_CLOEXEC,0);auto s=read_fd(fd);if(fd>=0)close(fd);return "DIRECT syscall openat:\n"+s;
#else
return "DIRECT syscall openat: unavailable";
#endif
}
static std::string direct_openat2(const char*p){
#ifdef SYS_openat2
struct open_how{uint64_t flags;uint64_t mode;uint64_t resolve;};open_how h{(uint64_t)(O_RDONLY|O_CLOEXEC),0,0};int fd=(int)syscall(SYS_openat2,AT_FDCWD,p,&h,sizeof(h));auto s=read_fd(fd);if(fd>=0)close(fd);return "DIRECT syscall openat2:\n"+s;
#else
return "DIRECT syscall openat2: unavailable";
#endif
}
static std::string libc_fopen(const char*p){FILE*f=fopen(p,"rb");if(!f)return "LIBC fopen:\nerrno="+std::to_string(errno);char b[4096];size_t n=fread(b,1,sizeof(b)-1,f);fclose(f);b[n]=0;return "LIBC fopen:\nread="+std::to_string(n)+"\n"+std::string(b,n);}
static std::string pread_test(const char*p){int fd=openat(AT_FDCWD,p,O_RDONLY|O_CLOEXEC);if(fd<0)return "pread: open errno="+std::to_string(errno);char b[4096];ssize_t n=pread(fd,b,sizeof(b)-1,0);if(n>0)b[n]=0;close(fd);return "pread after openat:\nread="+std::to_string(n)+"\n"+(n>0?std::string(b,n):"");}
static std::string mmap_test(const char*p){int fd=openat(AT_FDCWD,p,O_RDONLY|O_CLOEXEC);if(fd<0)return "mmap: open errno="+std::to_string(errno);struct stat st{};fstat(fd,&st);if(st.st_size<=0){close(fd);return "mmap: size="+std::to_string(st.st_size);}size_t len=(size_t)st.st_size;void*m=mmap(nullptr,len,PROT_READ,MAP_PRIVATE,fd,0);if(m==MAP_FAILED){int e=errno;close(fd);return "mmap: failed errno="+std::to_string(e);}size_t n=len<4096?len:4096;std::string s="mmap: OK size="+std::to_string(len)+"\n"+std::string((char*)m,n);munmap(m,len);close(fd);return s;}
static std::string ioctl_test(const char*p){int fd=openat(AT_FDCWD,p,O_RDONLY|O_CLOEXEC);if(fd<0)return "ioctl: open errno="+std::to_string(errno);int v=0;int r=ioctl(fd,FIONREAD,&v);int e=errno;close(fd);return "ioctl(FIONREAD): ret="+std::to_string(r)+" value="+std::to_string(v)+(r<0?" errno="+std::to_string(e):"");}
static std::string run(const char*p){std::string o="PID="+std::to_string(getpid())+"\nPATH="+std::string(p)+"\n\n";o+=libc_openat(p)+"\n\n";o+=libc_open(p)+"\n\n";o+=direct_openat(p)+"\n\n";o+=direct_openat2(p)+"\n\n";o+=libc_fopen(p)+"\n\n";o+=pread_test(p)+"\n\n";o+=mmap_test(p)+"\n\n";o+=ioctl_test(p);return o;}
extern "C" JNIEXPORT jstring JNICALL Java_com_erfansst_libtester_MainActivity_nativeTest(JNIEnv*env,jobject,jstring jp){const char*p=env->GetStringUTFChars(jp,nullptr);std::string s=run(p);env->ReleaseStringUTFChars(jp,p);return env->NewStringUTF(s.c_str());}
