## Almost-LZ77 compression

Given a description of the compressed data format, write code that compresses/decompresses files. 

 First, the compressed format:

A 0 bit followed by eight bits means just copy the eight bits to the output directly.

A 1 bit is followed by a pointer of 12 bits followed by a length encoded in 4 bits.  This is to be interpreted as "copy the <length> bytes from <pointer> bytes ago in the output to the current location".


For example:

"mahi mahi" can be compressed as:

<0,'m'><0,'a'><0,'h'><0,'i'><

0,' '><1,4,4>

Original size = 9 bytes, compressed = just under 8 bytes.

You don't need to produce optimal compression (hard), greedy matching is fine.  However, we want something that runs as fast as possible, without taking too much code (use your discretion).

The compressor and decompressor should take binary files as input and output.  If you're familiar with Lempel-Ziv compressors, this is a simplified LZ77 compressor. 


