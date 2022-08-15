import igraph as ig
import networkx as nx
import random as rd
import sys


import numpy as np
import matplotlib.pyplot as plt

graph_ig=ig.load('lngraph20211125connected.txt',format='pickle')
graph= graph_ig.to_networkx()



with open('graph.txt', 'w') as f:
    sys.stdout = f # Change the standard output to the file we created.
    print('# Name of the Graph:')
    print('LN 2021-11-25')
    print('# Number of Nodes:')
    print(graph.number_of_nodes()) 
    print('# Number of Edges:')
    print(graph.number_of_edges()) 
    print('')
    for node in graph.nodes(): 
        neighs = list(nx.all_neighbors(graph,node))
        neighs = list(dict.fromkeys(neighs)) 
        line = str(node) + ':'
        c = 0 
        for v in neighs:
            line = line + str(v)  
            c = c + 1
            if c < len(neighs):
                line = line + ';'
        print(line)

