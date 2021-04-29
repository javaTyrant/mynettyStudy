jps -l 查看java进程号

 jstat -gcutil 进程号 1000(时间) 500(次数,不加就是实时)

26900 

S0：Heap上的 Survivor space 0 段已使用空间的百分比
S1：Heap上的 Survivor space 1 段已使用空间的百分比
E： Heap上的 Eden space 段已使用空间的百分比
O： Heap上的 Old space 段已使用空间的百分比
P： Perm space 已使用空间的百分比
YGC：从程序启动到采样时发生Young GC的次数
YGCT：Young GC所用的时间(单位秒)
FGC：从程序启动到采样时发生Full GC的次数
FGCT：Full GC所用的时间(单位秒)
GCT：用于垃圾回收的总时间(单位秒)