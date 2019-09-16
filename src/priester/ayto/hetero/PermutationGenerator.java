package priester.ayto.hetero;

public class PermutationGenerator {
	private PermutationListener listener;

	public PermutationGenerator(PermutationListener listener) {
		this.listener = listener;
	}

	public void generate(int n) {
		int[] array = new int[n];
		for (int i = 0; i < n; i++)
			array[i] = i;

		generate(array, array.length);
		listener.onComplete();
	}

	private void generate(int[] array, int size) {
		if (size == 1) {
			listener.onNextPermutation(array);
			return;
		}

		for (int i = 0; i < size; i++) {
			generate(array, size - 1);
			if (size % 2 == 1)
				swap(array, 0, size - 1);
			else
				swap(array, i, size - 1);
		}
	}

	private void swap(int[] array, int i, int j) {
		int temp = array[i];
		array[i] = array[j];
		array[j] = temp;
	}

	public interface PermutationListener {
		public void onNextPermutation(int[] array);

		public void onComplete();
	}
}