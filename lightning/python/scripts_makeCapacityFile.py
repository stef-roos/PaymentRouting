import igraph as ig
import networkx as nx
import random as rd
import sys


import numpy as np
import matplotlib.pyplot as plt
import random as rn 

graph_ig=ig.load('lngraph20211125connected.txt',format='pickle')
graph= graph_ig.to_networkx()

#mode='both'
#mode='half'
mode='random'


with open('graph.txt_CREDIT_LINKS', 'w') as f:
    sys.stdout = f # Change the standard output to the file we created.
    print('# Graph Property Class')
    print('treeembedding.credit.CreditLinks')
    print('# Key')
    print('CREDIT_LINKS')
    for e,v,d in graph.edges(data=True):
        if (v < e) and (not graph.has_edge(v,e)):
           h = e
           e = v
           v = h    
        if (e < v):
            c = d['capacity']
            if mode=='both':
                print(str(e) + ' ' + str(v) + ' -' + str(c) + ' 0 ' + str(c)) 
            elif mode=='half':  
                c = c/2 
                print(str(e) + ' ' + str(v) + ' -' + str(c) + ' 0 ' + str(c))
            elif mode=='random':
                r1 = rn.uniform(0, c)
                r2 = c-r1
                print(str(e) + ' ' + str(v) + ' -' + str(r1) + ' 0 ' + str(r2))
            else: 
                print('Unknown mode')
