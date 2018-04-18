#!/bin/sh

NUM_RUNS=50

for i in {1..8}; do
  qsub -t 1-$NUM_RUNS:1 tree_based.sh ~/workspace/wsc2008/Set0${i}MetaData 2008-tree-based${i};
done
