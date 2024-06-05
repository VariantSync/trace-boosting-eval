import numpy
import pandas as pd
from tabulate import tabulate
import numpy as np
import matplotlib.pyplot as plt
from scipy import interpolate
import json
import os
import ast
import math

datasetName = "Marlin"

# Parsing json data


def parseDataRQ1(data):
    experiment = data.pop('experiment_properties')
    dataInSamples = {}
    l3 = data
    for l2, l2_data in l3.items():
        # Extract accuracy data
        accuracy_data = {l1: [runs["accuracy"] for runs in l1_data.values()]
                         for l1, l1_data in l2_data.items()}
        # Extract precision data
        precision_data = {l1: [runs["precision"]
                               for runs in l1_data.values()] for l1, l1_data in l2_data.items()}
        # Extract recall data
        recall_data = {l1: [runs["recall"] for runs in l1_data.values()]
                       for l1, l1_data in l2_data.items()}
        # Extract f1-score data
        f1_data = {l1: [runs["f1-score"] for runs in l1_data.values()]
                   for l1, l1_data in l2_data.items()}

        accuracy_df = pd.DataFrame(
            accuracy_data, index=list(l2_data.values())[0].keys())
        precision_df = pd.DataFrame(
            precision_data, index=list(l2_data.values())[0].keys())
        recall_df = pd.DataFrame(
            recall_data, index=list(l2_data.values())[0].keys())
        f1_score_df = pd.DataFrame(
            f1_data, index=list(l2_data.values())[0].keys())

        # Calculate averages
        averages = {}
        for l1, l1_data in l2_data.items():
            length = len(l1_data)
            sumAccuracy = compute_average(l1_data, "accuracy")
            # sum(run_data["accuracy"] for run_data in l1_data.values()) / length
            avg_accuracy = compute_average(l1_data, "accuracy")
            # sum(run_data["precision"] for run_data in l1_data.values()) / len(l1_data)
            avg_precision = compute_average(l1_data, "precision")
            # sum(run_data["recall"] for run_data in l1_data.values()) / len(l1_data)
            avg_recall = compute_average(l1_data, "recall")
            # sum(run_data["f1-score"] for run_data in l1_data.values()) / len(l1_data)
            avg_f1_score = compute_average(l1_data, "f1-score")
            averages[l1] = {"accuracy": avg_accuracy, "precision": avg_precision,
                            "recall": avg_recall, "f1-score": avg_f1_score}

        average_df = pd.DataFrame(averages)
        dataInSamples[l2] = {'average_df': average_df, 'accuracy_df': accuracy_df,
                             'precision_df': precision_df, 'recall_df': recall_df, 'f1_score_df': f1_score_df}
    return experiment, dataInSamples


def compute_average(l1_data, type):
    sum = 0.0
    count = 0.0
    for run_data in l1_data.values():
        current = run_data[type]
        if not math.isnan(current):
            sum += run_data[type]
            count += 1.0
        else:
            print("nan")
    return sum/(count)


# def parseDataRQ3(data):
#     experiment = data.pop('experiment_properties')
#     dataInSamples = {}
#     l4 = data
#     for l3, l3_data in l4.items():
#         for l2, l2_data in l3_data.items():
#             # Extract accuracy data
#             accuracy_data = {l1: [runs["accuracy"] for runs in l1_data.values()] for l1, l1_data in l2_data.items()}
#             # Extract precision data
#             precision_data = {l1: [runs["precision"] for runs in l1_data.values()] for l1, l1_data in l2_data.items()}
#             # Extract recall data
#             recall_data = {l1: [runs["recall"] for runs in l1_data.values()] for l1, l1_data in l2_data.items()}
#             # Extract f1-score data
#             f1_data = {l1: [runs["f1-score"] for runs in l1_data.values()] for l1, l1_data in l2_data.items()}
#
#             accuracy_df = pd.DataFrame(accuracy_data, index=list(l2_data.values())[0].keys())
#             precision_df = pd.DataFrame(precision_data, index=list(l2_data.values())[0].keys())
#             recall_df = pd.DataFrame(recall_data, index=list(l2_data.values())[0].keys())
#             f1_score_df = pd.DataFrame(f1_data, index=list(l2_data.values())[0].keys())
#
#             # Calculate averages
#             averages = {}
#             for l1, l1_data in l2_data.items():
#                 avg_accuracy = sum(run_data["accuracy"] for run_data in l1_data.values()) / len(l1_data)
#                 avg_precision = sum(run_data["precision"] for run_data in l1_data.values()) / len(l1_data)
#                 avg_recall = sum(run_data["recall"] for run_data in l1_data.values()) / len(l1_data)
#                 avg_f1_score = sum(run_data["f1-score"] for run_data in l1_data.values()) / len(l1_data)
#                 averages[l1] = {"accuracy": avg_accuracy, "precision": avg_precision, "recall": avg_recall, "f1-score": avg_f1_score}
#
#             average_df = pd.DataFrame(averages)
#             dataInSamples[l3+' - '+l2]= {'average_df':average_df, 'accuracy_df':accuracy_df, 'precision_df':precision_df, 'recall_df':recall_df, 'f1_score_df':f1_score_df}
#     return experiment, dataInSamples


def plotLines(experiment, average_df, accuracy_df, precision_df, recall_df, f1_score_df, marginAlpha=0.3, title='Test', rq=1):
    plt.figure(figsize=(10, 6))
    color = ['blue', 'orange', 'green', 'red']
    if rq == 1:
        # get range of percentage numbers
        x = np.array(ast.literal_eval(experiment['Percentage scenarios']))
    elif rq == 2:
        x = np.array(ast.literal_eval(experiment['Variants']))
    elif rq == 3:
        return
    #     x = np.array(ast.literal_eval(experiment['Standard Deviation scenarios']))
    # x = np.arange(len(average_df.columns))
    x_smooth = np.linspace(x.min(), x.max(), 100)

    # Plot accuracy
    y = average_df.loc['accuracy']
    print("x " + str(x) + " y " + str(y) + '\n')
    f = interpolate.interp1d(x, y, kind='linear')
    y_smooth = f(x_smooth)
    plt.plot(x_smooth, y_smooth, label='accuracy', color=color[0])
    # print(accuracy_df.min(axis=0))
    y1th = accuracy_df.min(axis=0)
    f = interpolate.interp1d(x, y1th, kind='linear')
    y1th_smooth = f(x_smooth)
    y2th = accuracy_df.max(axis=0)
    f = interpolate.interp1d(x, y2th, kind='linear')
    y2th_smooth = f(x_smooth)
    plt.fill_between(x_smooth, y1th_smooth, y2th_smooth,
                     color=color[0], alpha=marginAlpha)

    # Plot precision
    y = average_df.loc['precision']
    f = interpolate.interp1d(x, y, kind='cubic')
    y_smooth = f(x_smooth)
    plt.plot(x_smooth, y_smooth, label='precision', color=color[1])
    y1th = precision_df.min(axis=0)
    f = interpolate.interp1d(x, y1th, kind='cubic')
    y1th_smooth = f(x_smooth)
    y2th = precision_df.max(axis=0)
    f = interpolate.interp1d(x, y2th, kind='cubic')
    y2th_smooth = f(x_smooth)
    plt.fill_between(x_smooth, y1th_smooth, y2th_smooth,
                     color=color[1], alpha=marginAlpha)

    # Plot recall
    y = average_df.loc['recall']
    f = interpolate.interp1d(x, y, kind='cubic')
    y_smooth = f(x_smooth)
    plt.plot(x_smooth, y_smooth, label='recall', color=color[2])
    y1th = recall_df.min(axis=0)
    f = interpolate.interp1d(x, y1th, kind='cubic')
    y1th_smooth = f(x_smooth)
    y2th = recall_df.max(axis=0)
    f = interpolate.interp1d(x, y2th, kind='cubic')
    y2th_smooth = f(x_smooth)
    plt.fill_between(x_smooth, y1th_smooth, y2th_smooth,
                     color=color[2], alpha=marginAlpha)

    # Plot recall
    y = average_df.loc['f1-score']
    f = interpolate.interp1d(x, y, kind='cubic')
    y_smooth = f(x_smooth)
    plt.plot(x_smooth, y_smooth, label='f1-score', color=color[3])
    # y1th = f1_score_df.loc['1th run']
    y1th = f1_score_df.min(axis=0)
    f = interpolate.interp1d(x, y1th, kind='cubic')
    y1th_smooth = f(x_smooth)
    # y2th = f1_score_df.loc['2th run']
    y2th = f1_score_df.max(axis=0)
    f = interpolate.interp1d(x, y2th, kind='cubic')
    y2th_smooth = f(x_smooth)
    plt.fill_between(x_smooth, y1th_smooth, y2th_smooth,
                     color=color[3], alpha=marginAlpha)

    # xgrid = np.linspace(x.min(), x.max(), 5)
    # xgrid = sorted(set(np.append(x,xgrid).round()))
    # print(xgrid)
    # print(x)
    plt.grid(True, which='both', linestyle='-',
             linewidth=0.5, color='gray')  # Normal grid
    # Set x-axis tick locations
    plt.xticks(x, [str(scenario) for scenario in x])

    plt.title(title)
    if rq == 1:
        plt.xlabel('Percentage Mapping')
    elif rq == 2:
        plt.xlabel('Number Of Variants')
    elif rq == 3:
        return  # plt.xlabel('Standard Deviation Values')
    plt.ylabel('Percent')
    plt.legend()
    plt.grid(True)
    plt.show()

# Plotting Bars function


def plotBars(average_df, alpha=0.5, title='Test', rq=1):
    color = ['blue', 'orange', 'green', 'red']
    # Plotting the data
    average_df.plot(kind='bar', figsize=(10, 6), color=color, alpha=alpha)
    plt.title(title)
    if rq == 1:
        plt.xlabel('Percentage Mapping')
    elif rq == 2:
        plt.xlabel('Number Of Variants')
    elif rq == 3:
        return  # plt.xlabel('Standard Deviation Values')
    plt.ylabel('Percent')
    plt.xticks(rotation=0)
    plt.legend(title='Metric')
    plt.grid(axis='y')
    plt.show()

# Plotting Boxes function


def plotBoxs(data, title="Test", rq=1):
    data.boxplot(sym='o',               # Symbol to use for outliers
                 patch_artist=True,     # Whether to fill the boxes with colors
                 meanline=True,         # Whether to show the mean as a line
                 showmeans=False,       # Whether to show the mean as a point
                 showcaps=True,         # Whether to show caps on the ends of whiskers
                 showbox=True,          # Whether to show the box
                 showfliers=True,       # Whether to show outliers
                 widths=0.1,            # Width of the boxes
                 figsize=(10, 6),       # Size of the figure
                 grid=True,             # Whether to show grid lines

                 boxprops=dict(color='blue'),          # Color of the box
                 whiskerprops=dict(color='green'),     # Color of the whiskers
                 capprops=dict(color='red'),           # Color of the caps
                 # Color of the median line
                 medianprops=dict(color='orange'),
                 # Color and style of outliers
                 flierprops=dict(markerfacecolor='purple',
                                 marker='o', markersize=8),
                 )
    plt.title(title)
    plt.ylabel('Percent')
    if rq == 1:
        plt.xlabel('Percentage Mapping')
    elif rq == 2:
        plt.xlabel('Number Of Variants')
    elif rq == 3:
        return  # plt.xlabel('Standard Deviation Values')
    plt.savefig(fname='../results/plots/' + title, dpi='figure', format=None)
    plt.show()


with open(os.path.dirname(os.getcwd()) + '/results/rq1/experiment_result_' + datasetName + '.json', 'r') as file:
    datarq1 = json.load(file)
with open(os.path.dirname(os.getcwd()) + '/results/rq2/experiment_result_openvpn.json', 'r') as file:
    datarq2 = json.load(file)
# with open(os.path.dirname(os.getcwd())+'/data/result/rq3/experiment_result.json', 'r') as file:
#    datarq3 = json.load(file)

experimentrq1, dataInSamplesrq1 = parseDataRQ1(datarq1)
experimentrq2, dataInSamplesrq2 = parseDataRQ1(datarq2)
# experimentrq3, dataInSamplesrq3 = parseDataRQ3(datarq3)
print("RQ1", experimentrq1)
for k, d in dataInSamplesrq1.items():
    print('average_df')
    print(tabulate(d['average_df'], headers='keys', tablefmt='psql'))
    print('accuracy_df')
    print(tabulate(d['accuracy_df'], headers='keys', tablefmt='psql'))
    print('precision_df')
    print(tabulate(d['precision_df'], headers='keys', tablefmt='psql'))
    print('recall_df')
    print(tabulate(d['recall_df'], headers='keys', tablefmt='psql'))
    print('f1_score_df')
    print(tabulate(d['f1_score_df'], headers='keys', tablefmt='psql'))
print("RQ2", experimentrq2)
for k, d in dataInSamplesrq2.items():
    print('average_df')
    print(tabulate(d['average_df'], headers='keys', tablefmt='psql'))
    print('accuracy_df')
    print(tabulate(d['accuracy_df'], headers='keys', tablefmt='psql'))
    print('precision_df')
    print(tabulate(d['precision_df'], headers='keys', tablefmt='psql'))
    print('recall_df')
    print(tabulate(d['recall_df'], headers='keys', tablefmt='psql'))
    print('f1_score_df')
    print(tabulate(d['f1_score_df'], headers='keys', tablefmt='psql'))
# print("RQ3", experimentrq3)
# for k, d in dataInSamplesrq3.items():
#     print('average_df')
#     print(tabulate(d['average_df'], headers='keys', tablefmt='psql'))
#     print('accuracy_df')
#     print(tabulate(d['accuracy_df'], headers='keys', tablefmt='psql'))
#     print('precision_df')
#     print(tabulate(d['precision_df'], headers='keys', tablefmt='psql'))
#     print('recall_df')
#     print(tabulate(d['recall_df'], headers='keys', tablefmt='psql'))
#     print('f1_score_df')
#     print(tabulate(d['f1_score_df'], headers='keys', tablefmt='psql'))

for sample_size, sampleData in dataInSamplesrq1.items():
    # averages with deviation?
    plotLines(experimentrq1,
              sampleData['average_df'], sampleData['accuracy_df'], sampleData['precision_df'],
              sampleData['recall_df'], sampleData['f1_score_df'], title=sample_size)
    # averages
    plotBars(sampleData['average_df'].T,
             title=datasetName + ' ' + sample_size)

    # boxplots
    plotBoxs(sampleData['accuracy_df'],
             title=datasetName + '-' + sample_size + '-accuracy')
    plotBoxs(sampleData['precision_df'],
             title=datasetName + ' ' + sample_size + '-precision')
    plotBoxs(sampleData['recall_df'],
             title=datasetName + ' ' + sample_size + '-recall')
    plotBoxs(sampleData['f1_score_df'],
             title=datasetName + ' ' + sample_size + '-f1_score')
    print('------------------------------------------------------------------')

for percentage_size, percentageData in dataInSamplesrq2.items():
    plotLines(experimentrq2, percentageData['average_df'],
              percentageData['accuracy_df'], percentageData['precision_df'],
              percentageData['recall_df'], percentageData['f1_score_df'],
              title=percentage_size, rq=2)
    plotBars(percentageData['average_df'].T,
             title=datasetName + '-' + percentage_size, rq=2)
    plotBoxs(percentageData['accuracy_df'],     title=datasetName +
             '-' + percentage_size + '-accuracy', rq=2)
    plotBoxs(percentageData['precision_df'],    title=datasetName +
             '-' + percentage_size + '-precision', rq=2)
    plotBoxs(percentageData['recall_df'],
             title=datasetName + '-' + percentage_size + '-recall', rq=2)
    plotBoxs(percentageData['f1_score_df'],     title=datasetName +
             '-' + percentage_size + '-f1_score', rq=2)
    print('------------------------------------------------------------------')

# for s_p_size, s_p_Data in dataInSamplesrq3.items():
#     plotLines(experimentrq3, s_p_Data['average_df'],
#               s_p_Data['accuracy_df'], s_p_Data['precision_df'],
#               s_p_Data['recall_df'], s_p_Data['f1_score_df'],
#               title=s_p_size, rq=3)
#     plotBars(s_p_Data['average_df'].T, title=s_p_size, rq=3)
#     plotBoxs(s_p_Data['accuracy_df'], title=s_p_size+' accuracy', rq=3)
#     plotBoxs(s_p_Data['precision_df'], title=s_p_size+' precision', rq=3)
#     plotBoxs(s_p_Data['recall_df'], title=s_p_size+' recall', rq=3)
#     plotBoxs(s_p_Data['f1_score_df'], title=s_p_size+' f1_score', rq=3)
#     print('------------------------------------------------------------------')
