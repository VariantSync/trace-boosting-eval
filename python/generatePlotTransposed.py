import numpy
import pandas as pd
from pandas import DataFrame
from tabulate import tabulate
import numpy as np
import matplotlib.pyplot as plt
from scipy import interpolate
import json
import os
import ast
import math

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
            accuracy_data, index=list(l2_data.values())[0].keys()).T
        precision_df = pd.DataFrame(
            precision_data, index=list(l2_data.values())[0].keys()).T
        recall_df = pd.DataFrame(
            recall_data, index=list(l2_data.values())[0].keys()).T
        f1_score_df = pd.DataFrame(
            f1_data, index=list(l2_data.values())[0].keys()).T

        dataInSamples[l2] = {'accuracy_df': accuracy_df, 'precision_df': precision_df,
                             'recall_df': recall_df, 'f1_score_df': f1_score_df}
    return experiment, dataInSamples


def plotLines(experiment, dataset_name, sample_size, average_df, accuracy_df, precision_df, recall_df, f1_score_df, marginAlpha=0.3, title='Test', rq=1):
    plt.figure(figsize=(10, 6))
    color = ['blue', 'purple', 'green', 'red']
    if rq == 1:
        # get range of percentage numbers
        x = np.array([0, 5, 10, 15, 20, 25])
    x_smooth = np.linspace(x.min(), x.max(), 100)

    # interpolation = 'linear'
    interpolation = 'cubic'

    plot_accuracy = False
    plot_precision = False
    plot_recall = True
    plot_f_score = False

    # Plot accuracy
    if plot_accuracy:
        plot_dataframe('average accuracy improvement', accuracy_df,
                       color[0], interpolation, marginAlpha, x, x_smooth)

    # Plot precision
    if plot_precision:
        plot_dataframe('average precision improvement', precision_df,
                       color[1], interpolation, marginAlpha, x, x_smooth)

    # Plot recall
    if plot_recall:
        plot_dataframe('average recall improvement', recall_df,
                       color[2], interpolation, marginAlpha, x, x_smooth)
        # Plot the investment line
        # plt.plot([0, 5, 10, 15, 20, 25], [0, 5, 10, 15, 20, 25], linestyle='--', label='1-to-1 return on investment')

    # # Plot f1
    if plot_f_score:
        plot_dataframe('average f1 improvement', f1_score_df,
                       color[3], interpolation, marginAlpha, x, x_smooth)

    plt.grid(True, which='both', linestyle='-',
             linewidth=0.5, color='gray')  # Normal grid
    # Set x-axis tick locations
    plt.xticks(x, [str(scenario) for scenario in x])

    plt.ylim(0, 100)
    plt.title(title)
    if rq == 1:
        plt.xlabel('Percentage Mapping')
    plt.ylabel('Improvement in Percentage Points')
    plt.legend()
    plt.grid(True)
    plt.savefig(fname='../results/plots/relative/' + dataset_name +
                '_lineplot_' + sample_size, dpi='figure', format=None)
    plt.show()


def plot_dataframe(label, accuracy_df, color, interpolation, marginAlpha, x, x_smooth):
    y = accuracy_df.mean(axis=0)
    y *= 100
    f = interpolate.interp1d(x, y, kind=interpolation)
    y_smooth = f(x_smooth)
    plt.plot(x_smooth, y_smooth, label=label, color=color)
    # Use the minimum
    # y1th = 100 * recall_df.min(axis=0)
    # Use the deviation
    y1th = y - 100 * accuracy_df.std(axis=0)
    f = interpolate.interp1d(x, y1th, kind=interpolation)
    y1th_smooth = f(x_smooth)
    # Use the maximum
    # y2th = 100 * recall_df.max(axis=0)
    # Use the deviation
    y2th = y + 100 * accuracy_df.std(axis=0)
    f = interpolate.interp1d(x, y2th, kind=interpolation)
    y2th_smooth = f(x_smooth)
    plt.fill_between(x_smooth, y1th_smooth, y2th_smooth,
                     color=color, alpha=marginAlpha)


# Plotting Bars function
def plotBars(average_df, alpha=0.5, title='Test', rq=1):
    color = ['blue', 'orange', 'green', 'red']
    # Plotting the data
    average_df.plot(kind='bar', figsize=(10, 6), color=color, alpha=alpha)
    plt.title(title)
    if rq == 1:
        plt.xlabel('Percentage Mapping')
    plt.ylabel('Percent')
    plt.xticks(rotation=0)
    plt.legend(title='Metric')
    plt.grid(axis='y')
    plt.show()

# Plotting Boxes function


def plotBoxs(data, title, ylabel, filename="Test", rq=1):
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
    plt.ylabel(ylabel)
    plt.ylim(0, 1)
    if rq == 1:
        plt.xlabel('Proactive Tracing Percentage')
    plt.savefig(fname='../results/plots/final/' +
                filename + ".pdf", dpi='figure', format="pdf")
    plt.show()


def convert_to_relative_results(data: dict):
    for _, results in data.items():
        accuracy_df = results['accuracy_df']
        precision_df = results['precision_df']
        recall_df = results['recall_df']
        f1_score_df = results['f1_score_df']
        convert_dataframe(accuracy_df)
        convert_dataframe(precision_df)
        convert_dataframe(recall_df)
        convert_dataframe(f1_score_df)
        avg_accuracy = average_dataframe(accuracy_df)
        avg_precision = average_dataframe(precision_df)
        avg_recall = average_dataframe(recall_df)
        avg_f1_score = average_dataframe(f1_score_df)
        averages = {"accuracy": avg_accuracy, "precision": avg_precision, "recall": avg_recall,
                    "f1-score": avg_f1_score}
        average_df = pd.DataFrame(averages)

        # Remove the no longer valid average dataframe
        results['average_df'] = average_df


def convert_dataframe(df: DataFrame):
    baseline = df["0%"].copy()
    for (perc, percentage_runs) in df.items():
      #  if perc == "0%":
      #      continue
        for runId in [str(i) + "run" for i in range(1, len(percentage_runs)+1)]:
            baseline_value = baseline[runId]
            absolute_value = percentage_runs[runId]
            # Update the value with a relative one
            percentage_runs[runId] = absolute_value - baseline_value
    # Remove the baseline values
    # df.pop("0%")


def average_dataframe(df: DataFrame):
    averages = {}
    for (perc, percentage_runs) in df.items():
        total = 0.0
        for runId in [str(i) + "run" for i in range(1, len(percentage_runs)+1)]:
            total += percentage_runs[runId]
        average = total / len(percentage_runs)
        averages[perc] = average
    return averages


def print_tables(experiment, data):
    print("RQ1", experiment)
    for k, d in data.items():
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


def main():
    subjects = ["argouml-spl", "busybox", "vim", "openvpn", "Marlin"]
    for datasetName in subjects:
        with open(os.path.dirname(os.getcwd()) + '/results/experiment_result_' + datasetName + '.json', 'r') as file:
            raw_data = json.load(file)
        experiment, data = parseDataRQ1(raw_data)

        # Print the dataframes to the console
        # print_tables(experiment, data)

        # convert_to_relative_results(data)

        for sample_size, sampleData in data.items():
            # averages with deviation?
            sample_size = sample_size.split()[0]
            # title = "Average recall improvement with " + sample_size + " variants"
            # plotLines(experiment,
            #          datasetName,
            #          sample_size,
            #          sampleData['average_df'],
            #          sampleData['accuracy_df'], sampleData['precision_df'],
            #          sampleData['recall_df'], sampleData['f1_score_df'], title=title)

            # boxplots
            title = ''
            plotBoxs(sampleData['accuracy_df'], title=title, ylabel='Accuracy',
                     filename="accuracy/" + datasetName + '-' + sample_size)
            plotBoxs(sampleData['precision_df'], title=title, ylabel='Precision',
                     filename="precision/" + datasetName + '-' + sample_size)
            plotBoxs(sampleData['recall_df'], title=title, ylabel='Recall',
                     filename="recall/" + datasetName + '-' + sample_size)
            plotBoxs(sampleData['f1_score_df'], title=title, ylabel='F1 Score',
                     filename="f1_score/" + datasetName + '-' + sample_size)
            print('------------------------------------------------------------------')


if __name__ == "__main__":
    main()
