import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MatrixMultiplicationDriver {

    public static class MatrixMapper extends Mapper<Object, Text, Text, Text> {
        @Override
        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            String[] parts = value.toString().split("\\s+");
            String matrixName = parts[0];
            int row = Integer.parseInt(parts[1]);
            int col = Integer.parseInt(parts[2]);
            int val = Integer.parseInt(parts[3]);

            // Emit key-value pairs for matrix multiplication
            if (matrixName.equals("A")) {
                // For matrix A, emit (i,j) for all possible j
                for (int j = 0; j < 2; j++) { // assuming 2x2 matrices
                    context.write(new Text(row + "," + j), new Text("A," + col + "," + val));
                }
            } else if (matrixName.equals("B")) {
                // For matrix B, emit (i,j) for all possible i
                for (int i = 0; i < 2; i++) { // assuming 2x2 matrices
                    context.write(new Text(i + "," + col), new Text("B," + row + "," + val));
                }
            }
        }
    }

    public static class MatrixReducer extends Reducer<Text, Text, Text, IntWritable> {
        @Override
        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            Map<Integer, Integer> aMap = new HashMap<>();
            Map<Integer, Integer> bMap = new HashMap<>();

            for (Text val : values) {
                String[] parts = val.toString().split(",");
                String matrixType = parts[0];
                int index = Integer.parseInt(parts[1]);
                int value = Integer.parseInt(parts[2]);

                if (matrixType.equals("A")) {
                    aMap.put(index, value);
                } else {
                    bMap.put(index, value);
                }
            }

            // Multiply matching indices
            int result = 0;
            for (int k : aMap.keySet()) {
                if (bMap.containsKey(k)) {
                    result += aMap.get(k) * bMap.get(k);
                }
            }

            context.write(key, new IntWritable(result));
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: MatrixMultiplicationDriver <input path> <output path>");
            System.exit(-1);
        }

        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "Matrix Multiplication");

        job.setJarByClass(MatrixMultiplicationDriver.class);
        job.setMapperClass(MatrixMapper.class);
        job.setReducerClass(MatrixReducer.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
