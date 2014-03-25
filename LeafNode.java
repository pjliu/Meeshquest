//package cmsc420.heptatrie;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class LeafNode<K, V> extends Node<K, V> {
	protected List<K> keys;

	protected List<V> values;

	protected int leafOrder;

	public LeafNode(Comparator<K> comparator, int leafOrder) {
		super(comparator, NodeType.LEAF);
		this.keys = new ArrayList<K>();
		this.values = new ArrayList<V>();
		this.leafOrder = leafOrder;
	}

	protected LeafNode(Comparator<K> comparator, int leafOrder, List<K> keys,
			List<V> values) {
		super(comparator, NodeType.LEAF);
		this.keys = new ArrayList<K>(keys);
		this.values = new ArrayList<V>(values);
		this.leafOrder = leafOrder;
	}

	public int size() {
		return keys.size();
	}

	public boolean isFull() {
		return keys.size() == leafOrder;
	}

	protected boolean isOverFull() {
		return keys.size() > leafOrder;
	}
	
	private boolean isUnderHalfFull() {
		return keys.size()*2 < leafOrder;
	}
		
	public List<K> getKeys() {
		return keys;
	}

	public List<V> getValues() {
		return values;
	}	
	
	public void put(K key, V value) {
		int ind = 0;
		K guide = null;
		for (K k : keys) {
			if (cmp(key, k) <= 0) {
				guide = k;
				break;
			}
			++ind;
		}

		int compare = (guide == null) ? -1 : cmp(key, guide);
		if (compare != 0) {
			keys.add(ind, key);
			values.add(ind, value);
		} else {
			values.set(ind, value);
		}

		if (isOverFull()) {
			split();
		}
	}

	public V get(K key) {
		if (keys.contains(key)) {
			return values.get(keys.indexOf(key));
		}
		return null;
	}

	public V remove(K key) {
		V result = null;
		int ind = 0;
		K guide = null;
		for (K k : keys) {
			if (cmp(key, k) <= 0) {
				guide = k;
				break;
			}
			++ind;
		}
		int compare = (guide == null) ? -1 : cmp(key, guide);
		if (compare != 0) {
			result = null;
		} else {
			keys.remove(ind);
			result = values.remove(ind);
		}
				
		if (isUnderHalfFull()){
			merge();
		}
		return result;
	}	

	protected void split() {
		int halfKeys = keys.size() / 2;
		LeafNode<K, V> left = new LeafNode<K, V>(comparator, leafOrder,
				keys.subList(0, halfKeys), values.subList(0, halfKeys));
		LeafNode<K, V> right = new LeafNode<K, V>(comparator, leafOrder,
				keys.subList(halfKeys, keys.size()), values.subList(halfKeys,
						values.size()));
		left.setLeft(getLeft());
		left.setRight(right);
		getLeft().setRight(left);
		left.setParent(getParent());
		right.setLeft(left);
		right.setRight(getRight());
		getRight().setLeft(right);
		right.setParent(getParent());
		parent.insertKid(this, right.keys.get(0), left, right);
	}
	
	protected void merge() {
		if (parent != null) {
			parent.deleteKid(this);
			Node<K,V> prev = null;
			Node<K,V> temp = parent;
			while (temp != null && temp.getType() != NodeType.ROOT) {
				prev = temp;
				temp = temp.getParent();
			}
			if (prev != null) {				
				for (int j=0; j<size(); j++) {
					prev.put((K)keys.get(j), (V)values.get(j));
				}
			}
		}
	}
		
	protected Iterator<K> leafKeyIterator() {
		return new LeafKeyIterator();
	}

	private class LeafKeyIterator implements Iterator<K> {
		private int entryNum = 0;

		public boolean hasNext() {
			return entryNum < keys.size();
		}

		public K next() {
			return keys.get(entryNum++);
		}

		public void remove() {
		}
	}

	public void addToXmlDoc(Document doc, Element parent) {
		Element thisNode = doc.createElement("leaf");	
		for (int i = 0; i < keys.size(); ++i) {
			Element entry = doc.createElement("entry");
			entry.setAttribute("key", keys.get(i).toString());
			entry.setAttribute("value", values.get(i).toString());
			thisNode.appendChild(entry);
		}
		parent.appendChild(thisNode);
	}
	
	public String toString() {
		return keys.toString();
	}
}
