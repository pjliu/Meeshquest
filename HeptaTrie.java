//package cmsc420.heptatrie;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class HeptaTrie<K, V> implements SortedMap<K, V> {
	private RootNode<K,V> root;

	private int size = 0;

	private final int leafOrder;

	protected int modCount = Integer.MIN_VALUE;

	class DefaultComparator implements Comparator<K> {
		@SuppressWarnings("unchecked")
		public int compare(K o1, K o2) {
			if (o1 == null) {
				if (o2 == null) {
					return 0;
				} else {
					return -1;
				}
			} else {
				if (o2 == null) {
					return 1;
				} else {
					return ((Comparable<K>) o1).compareTo(o2);
				}
			}
		}
	}

	public HeptaTrie(Comparator<K> comparator, int leafOrder) {
		this.leafOrder = leafOrder;
		if (comparator == null) {
			comparator = new DefaultComparator();
		}
		root = new RootNode(comparator, leafOrder);
	}

	public Comparator<? super K> comparator() {
		return root.getComparator();
	}
	
	public K firstKey() {
		if (isEmpty()) {
			throw new NoSuchElementException();
		}

		return root.getFirstLeaf().getKeys().get(0);
	}

	public SortedMap<K, V> headMap(Object arg0) {
		throw new UnsupportedOperationException();
	}

	public K lastKey() {
		if (isEmpty()) {
			throw new NoSuchElementException();
		}

		LeafNode<K, V> l = root.getLastLeaf();
		return l.getKeys().get(l.size() - 1);
	}

	public SortedMap<K, V> subMap(K arg0, K arg1) {
		throw new UnsupportedOperationException();
	}

	public SortedMap<K, V> tailMap(K arg0) {
		throw new UnsupportedOperationException();
	}

	public void clear() {
		root = new RootNode(root.getComparator(), leafOrder);
		size = 0;
		modCount++;
	}

	@SuppressWarnings("unchecked")
	public boolean containsKey(Object key) {
		if (key == null) {
			throw new NullPointerException();
		}

		return root.contains((K) key);
	}

	public boolean containsValue(Object arg0) {
		for (Map.Entry<K, V> entry : entrySet()) {
			if (arg0.equals(entry.getValue())) {
				return true;
			}
		}
		return false;
	}

	public Set<Map.Entry<K, V>> entrySet() {
		return new EntrySet();
	}

	@SuppressWarnings("unchecked")
	public V get(Object key) {
		if (key == null) {
			throw new NullPointerException();
		}
		return root.get((K) key);
	}

	public boolean isEmpty() {
		return size == 0;
	}

	public Set<K> keySet() {
		throw new UnsupportedOperationException();
	}

	public V put(K key, V value) {
		if (key == null) {
			throw new NullPointerException();
		}

		V oldVal = get(key);
		if (!root.contains(key) && size != Integer.MAX_VALUE) {
			size++;
		}

		root.put(key, value);
		modCount++;
		return oldVal;
	}

	public void putAll(Map<? extends K, ? extends V> map) {
		for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
			put(entry.getKey(), entry.getValue());
		}
	}

	public V remove(Object arg0) {
		K key = (K)arg0;
		if (key == null) {
			throw new NullPointerException();
		}
		
		if (root.contains(key) || size > 0) {
			size--;			
		}
		
		V oldVal = root.remove(key);
		modCount++;
		return oldVal;
	}

	public int size() {
		return size;
	}

	public Collection<V> values() {
		throw new UnsupportedOperationException();
	}

	public boolean equals(Object arg0) {
		if (arg0 instanceof Map) {
			@SuppressWarnings("unchecked")
			Map<K, V> m1 = (Map<K, V>) arg0;
			return m1.entrySet().equals(entrySet());
		}
		return false;
	}

	public int hashCode() {
		return entrySet().hashCode();
	}

	public void addToXmlDoc(Document doc, Element parentNode) {
		Element heptaTrie = doc.createElement("HeptaTrie");
		heptaTrie.setAttribute("cardinality", Integer.toString(size()));
		heptaTrie.setAttribute("leafOrder", Integer.toString(leafOrder));
		root.addToXmlDoc(doc, heptaTrie);
		parentNode.appendChild(heptaTrie);
	}

	private class RootNode<K,V> extends Node<K, V> {
		private Node<K, V> me;

		private EndNode<K, V> first;

		private EndNode<K, V> last;

		private int leafOrder;
		
		private int height = 1;

		public RootNode(Comparator<K> comparator, int leafOrder) {
			super(comparator, NodeType.ROOT);
			this.leafOrder = leafOrder;
			LeafNode<K, V> tmp = new LeafNode<K, V>(comparator, this.leafOrder);
			this.first = new EndNode<K, V>();
			this.last = new EndNode<K, V>();
			this.first.setRight(tmp);
			this.last.setLeft(tmp);
			tmp.setLeft(first);
			tmp.setRight(last);
			this.me = tmp;
		}
		
		public Node<K,V> getMe() {
			return me;
		}

		public V get(K key) {
			return me.get(key);
		}

		public V remove(K key) {
			V obj = me.remove(key);
			if (me.getParent() == null && me.getType() == NodeType.GUIDE && ((GuideNode)me).size() == 1) {
				me = (Node<K,V>)((GuideNode)me).getKids().get(0);
				me.setParent(null);
			}
			return obj;
		}

		public void put(K key, V value) {
			if (me.isFull()) {
				// dummy new root
				GuideNode<K, V> newRoot = new GuideNode<K, V>(
						me.getComparator());
				newRoot.setLeft(new EndNode<K, V>());
				newRoot.setRight(new EndNode<K, V>());
				newRoot.getLeft().setRight(newRoot);
				newRoot.getRight().setLeft(newRoot);
				me.setParent(newRoot);
				me.put(key, value);
				if (newRoot.size() > 0) {
					me = newRoot;
					height++;
				} else {
					me.setParent(null);
				}
			} else {			
				me.put(key, value);
			}
		}

		public LeafNode<K, V> getFirstLeaf() {
			return (LeafNode<K, V>) first.getRight();
		}

		public LeafNode<K, V> getLastLeaf() {
			return (LeafNode<K, V>) last.getLeft();
		}

		public void addToXmlDoc(Document doc, Element parent) {
			me.addToXmlDoc(doc, parent);
		}
	}


	private class Entry implements Map.Entry<K, V> {
		private K key;
		private EntrySet.EntryIterator srcItr;

		public Entry(K key, EntrySet.EntryIterator srcItr) {
			this.key = key;
			this.srcItr = srcItr;
		}

		public K getKey() {
			return key;
		}

		public V getValue() {
		return get(key);
		}

		public V setValue(V arg0) {
			if (srcItr == null) {
				throw new IllegalStateException(
						"this entry is readonly because it is not created by an iterator");
			}
			V ret = put(key, arg0);
			srcItr.setModCount(HeptaTrie.this.modCount);
			return ret;
		}

		public boolean equals(Object arg0) {
			if (arg0 instanceof Map.Entry) {
				@SuppressWarnings("unchecked")
				Map.Entry<K, V> e2 = (Map.Entry<K, V>) arg0;
				return (key == null ? e2.getKey() == null : key.equals(e2.getKey()))
						&& (getValue() == null ? e2.getValue() == null
								: getValue().equals(e2.getValue()));
			}
			return false;
		}

		public int hashCode() {
			return (key == null ? 0 : key.hashCode())
					^ (getValue() == null ? 0 : getValue().hashCode());
		}
	}

	private class EntrySet implements Set<Map.Entry<K, V>> {
		public boolean add(Map.Entry<K, V> arg0) {
			throw new UnsupportedOperationException();
		}

		public boolean addAll(Collection<? extends Map.Entry<K, V>> arg0) {
			throw new UnsupportedOperationException();
		}

		public void clear() {
			HeptaTrie.this.clear();
		}

		public boolean contains(Object entry) {
			if (entry instanceof Map.Entry) {
				@SuppressWarnings("unchecked")
				Map.Entry<K, V> e1 = (Map.Entry<K, V>) entry;
				Object value = HeptaTrie.this.get(e1.getKey());
				return e1.getValue() == null ? value == null : e1.getValue()
						.equals(value);
			}
			return false;
		}

		public boolean containsAll(Collection<?> entries) {
			boolean flag = true;
			for (Object o : entries) {
				flag = flag && contains(o);
			}
			return flag;
		}

		public boolean isEmpty() {
			return HeptaTrie.this.isEmpty();
		}

		public Iterator<Map.Entry<K, V>> iterator() {
			return new EntryIterator();
		}

		@SuppressWarnings("unchecked")
		public boolean remove(Object entry) {
			if (entry instanceof Map.Entry) {
				return HeptaTrie.this.remove(((Entry) entry).getKey()) != null;
			}
			return false;
		}

		public boolean removeAll(Collection<?> entries) {
			boolean flag = false;
			for (Object o : entries) {
				flag = remove(o) || flag;
			}
			return flag;
		}

		public boolean retainAll(Collection<?> entries) {
			Iterator<Map.Entry<K, V>> it = new EntryIterator();
			boolean flag = false;
			while (it.hasNext()) {
				if (!entries.contains(it.next())) {
					it.remove();
					flag = true;
				}
			}
			return flag;
		}

		public int size() {
			return HeptaTrie.this.size();
		}

		public Object[] toArray() {
			Object[] arr = new Object[size()];
			Iterator<Map.Entry<K, V>> it = new EntryIterator();
			for (int i = 0; it.hasNext(); i++) {
				arr[i] = it.next();
			}
			return arr;
		}

		@SuppressWarnings("unchecked")
		public <T> T[] toArray(T[] array) {
			if (array.length < size())
				array = (T[]) Array.newInstance(array.getClass()
						.getComponentType(), size());

			Iterator<Map.Entry<K, V>> it = new EntryIterator();
			for (int i = 0; it.hasNext(); i++) {
				array[i] = (T) it.next();
			}
			return array;
		}

		public boolean equals(Object arg0) {
			if (arg0 instanceof Set) {
				@SuppressWarnings("unchecked")
				Set<Map.Entry<K, V>> s1 = (Set<Map.Entry<K, V>>) arg0;
				if (s1.size() != size()) {
					return false;
				}

				Iterator<Map.Entry<K, V>> it = s1.iterator();
				while (it.hasNext()) {
					if (!contains(it.next())) {
						return false;
					}
				}
				return true;
			}
			return false;
		}

		public int hashCode() {
			int hashCode = 0;
			for (Map.Entry<K, V> entry : this) {
				hashCode += entry.hashCode();
			}
			return hashCode;
		}

		private class EntryIterator implements Iterator<Map.Entry<K, V>> {
			private Entry curr, prev;
			private LeafNode<K, V> currNode;
			private Iterator<K> it;
			private int modCount = HeptaTrie.this.modCount;

			public EntryIterator() {
				if (HeptaTrie.this.isEmpty())
					return;

				this.currNode = root.getFirstLeaf();
				this.it = currNode.leafKeyIterator();

				while (curr == null
						|| HeptaTrie.this.comparator().compare(curr.getKey(),
								HeptaTrie.this.firstKey()) < 0) {
					if (!it.hasNext()) {
						if (currNode.getRight() instanceof EndNode)
							return;

						currNode = (LeafNode<K, V>) currNode.getRight();
						it = currNode.leafKeyIterator();
						continue;
					}

					curr = new Entry(it.next(), this);
				}
			}

			public boolean hasNext() {
				if (modCount != HeptaTrie.this.modCount) {
					throw new ConcurrentModificationException();
				}
				return curr != null;
			}

			public Map.Entry<K, V> next() {
				if (modCount != HeptaTrie.this.modCount)
					throw new ConcurrentModificationException();

				if (curr == null)
					throw new NoSuchElementException();

				if (!it.hasNext() && !(currNode.getRight() instanceof EndNode)) {
					currNode = (LeafNode<K, V>) currNode.getRight();
					it = currNode.leafKeyIterator();
					return next();
				}
				prev = curr;
				if (it.hasNext() && curr != null
						&& HeptaTrie.this.lastKey() != null
						&& cmp(curr.getKey(), HeptaTrie.this.lastKey()) < 0)
					curr = new Entry(it.next(), this);
				else
					curr = null;

				return prev;
			}

			public void remove() {
				if (modCount != HeptaTrie.this.modCount) {
					throw new ConcurrentModificationException();
				}
				HeptaTrie.this.remove(prev.getKey());
				modCount = HeptaTrie.this.modCount;
			}

			public void setModCount(int modCount) {
				this.modCount = modCount;
			}

			private int cmp(K k1, K k2) {
				return HeptaTrie.this.comparator().compare(k1, k2);
			}
		}
	}
	
        public static void main(String[] args) {

			HeptaTrie tree = new HeptaTrie<String, City>(new StringComparator(), 8);

			int count = 1000;
			City[] testCities = new City[count];			
			for (int i = 0; i < count; i++) {
				testCities[i] = new City("city"+i, i, i, i, 0, "black");
                tree.put(testCities[i].getName(), testCities[i]);
			}
	
	
			for (int i = 0; i < count; i++) {
				String removedKey = testCities[i].getName();
			    City removedCity = (City)tree.remove(removedKey);


				boolean verifyKey = tree.containsKey(removedKey);
				if (verifyKey == true) {
					System.out.println("key is still there!!!! key="+removedKey);
				}

				boolean verifyValue = tree.containsValue(removedCity);
				if (verifyValue == true) {
					System.out.println("value is still there!!!! value="+removedCity+" key="+removedKey);
				}
			
				for (int j=i+1; j<count; j++) {
					boolean contKey = tree.containsKey(testCities[j].getName());
					if (contKey== false) {
						System.out.println("lost key"+testCities[j].getName());
					}	
				}
			}
						
			try {
				Document results = XmlUtility.getDocumentBuilder().newDocument();
				Element testNode = results.createElement("test");				
				tree.addToXmlDoc(results, testNode);
				results.appendChild(testNode);
				XmlUtility.print(results);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}	
	
}
