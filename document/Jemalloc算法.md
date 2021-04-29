优势:, **jemalloc最大的优势还是其强大的多核/多线程分配能力**

首先, 先给出一个整体的概念. jemalloc对内存划分按照如下**由高到低**的顺序:

1. 内存是由一定数量的arenas进行管理.
2. 一个arena被分割成若干chunks, 后者主要负责记录bookkeeping（记录信息）.
3. chunk内部又包含着若干runs, 作为分配小块内存的基本单元.
4. run由pages组成, 最终被划分成一定数量的regions,
5. 对于small size的分配请求来说, 这些region就相当于user memory.

![这里写图片描述](https://img-blog.csdn.net/20161205174424849)