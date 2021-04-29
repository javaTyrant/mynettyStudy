## 2.1 netty内存池的主要数据结构

### PoolArena

PoolArena是分配算法的包装，对外部来说，**它是逻辑上的一块内存**。但实际上它是由**多个PoolChunk**组成的。为了提高效率，**每个线程都会持有2个PoolArena(一个代表堆内存，一个代表直接内存)**，**分配时从自己持有的PoolArena分配内存，最大限度地减少并发情况下的资源竞争，从而提高分配效率。**

### PoolChunk

PoolChunk是真正的一块**连续的内存**，几乎重要的分配算法，都是在PoolChunk实现的。每块PoolChunk的大小是**16M**。
PoolChunk又切为**2048个page**，page没有对应的数据结构，只有逻辑上的概念，netty的每个page大小是8k。

### PoolSubpage

PoolSubpage代表小于**8k**的内存，它是对一个page的分割。PoolSubpage可以里面还继续分为tiny和small。小于512bytes的为tiny，tiny最小可以是16bytes，这是netty内存池能分配的**最小粒度**的内存单元。大于等于512bytes并且小于8k的称为small。

### PoolThreadCache

PoolThreadCache是**内存分配的缓冲**，缓冲了tiny、small、normal三个大小级别的内存单元。每个线程都有自己的PoolThreadCache，**当分配内存时，首先从缓冲分配，从缓冲分配不到了，再从空闲的内存分配。**

## 2.2 netty内存池分配粒度的分级

首先，netty不会按申请多少分配多少的方式进行内存分配，采用的是**best fit**的方式。打个比方，如果申请的是9000bytes，那netty实际会分配出两页8192*2=16384bytes的内存大小，存在7384bytes的浪费，这也即所谓的内存**internal fragmentation**(直译：内碎片)。后续的文章会提到netty会如何尽量利用这个被浪费掉的空间。

为了尽量减少内存的内碎片造成的浪费，netty把内存粒度分配为许多个大小级别，最小的为16bytes，最大的一直到可用的最大内存为止。而这些粒度级别又进一步分为4大类：
（1）**huge**是大于16M的内存粒度，huge没有分配缓冲，分配huge时，直接从操作系统内存或者从堆内存分配申请的大小，huge的分配算法是最简单直接的。

（2）**normal**，小于等于一个chunk(即16M)，并且大于等于一个page(即8k)的内存粒度称为normal。normal的分配使用了buddy算法，在PoolChunk类实现。

（3）small和tiny，因为它们的分配算法相同，所以放到一起。关于它们的大小范围，见前文。
        从空闲内存分配small或tiny大小的内存粒度时，先从chunk找到一个空闲的page，再将page按申请的大小切割为subpage，并用一个bitmap(位图)存储每个subpage的分配情况。然后分配一份可用的subpage。其实对于small和tiny的分配，使用的是slab分配算法，后续文章将仔细分析。

从上文可知，对huge、normal、small/tiny各个粒度大小的内存，分配的算法差异很大，后面将分别详解。

## 3 与jemalloc的最大差别

netty的内存池分配技术，与jemalloc的最大差别，在于元数据的保存方式。

所谓元数据，是辅助进行内存分配和释放顺利完成所需要的数据结构，这部分数据结构也是需要消耗内存的。

jemalloc直接将所申请内存的一部分用来存放元数据；而netty是申请多少内存就得到多少内存，**元数据是另外消耗JVM的堆内存来保存的。**
