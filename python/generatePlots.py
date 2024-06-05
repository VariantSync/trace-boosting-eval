import pandas as pd
import matplotlib.pyplot as plt
import json
import os


def parseData(data):
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


def plotBoxs(data, title, ylabel, filename="Test"):
    plt.figure(figsize=(10, 6))  # Create a new figure
    data.boxplot(sym='o',               # Symbol to use for outliers
                 patch_artist=True,     # Whether to fill the boxes with colors
                 meanline=True,         # Whether to show the mean as a line
                 showmeans=False,       # Whether to show the mean as a point
                 showcaps=True,         # Whether to show caps on the ends of whiskers
                 showbox=True,          # Whether to show the box
                 showfliers=True,       # Whether to show outliers
                 widths=0.1,            # Width of the boxes

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
    plt.xlabel('Proactive Tracing Percentage')

    # Create the directory if it does not exist
    output_dir = '../results/plots/'

    output_file = os.path.join(output_dir, filename + ".pdf")
    print('save plot to ' + output_file)
    parent_dir = os.path.dirname(output_file)
    if not os.path.exists(parent_dir):
        os.makedirs(parent_dir)

    # Use the output_file variable
    plt.savefig(fname=output_file, dpi='figure', format="pdf")
    plt.close()  # Close the figure to prevent reusing the same plot


def main():
    subjects = ["argouml-spl", "busybox", "vim", "openvpn", "Marlin"]

    for datasetName in subjects:
        file_path = os.path.dirname(
            os.getcwd()) + '/results/experiment_result_' + datasetName + '.json'
        if os.path.exists(file_path):
            with open(file_path, 'r') as file:
                raw_data = json.load(file)
                experiment, data = parseData(raw_data)
        else:
            print('No results for ' + datasetName)
            continue

        print('--------------- create plots for ' +
              datasetName + ' ------------------------')
        for sample_size, sampleData in data.items():
            # averages with deviation?
            sample_size = sample_size.split()[0]
            print('----- sample size: ' + sample_size + ' -----')

            # boxplots
            title = ''
            plotBoxs(sampleData['precision_df'], title=title, ylabel='Precision',
                     filename="precision/" + datasetName + '-' + sample_size)
            plotBoxs(sampleData['recall_df'], title=title, ylabel='Recall',
                     filename="recall/" + datasetName + '-' + sample_size)


if __name__ == "__main__":
    main()
