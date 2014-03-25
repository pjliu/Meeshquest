//package cmsc420.heptatrie;

import java.util.Comparator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public abstract class Node<K, V> {
	public enum NodeType {
		GUIDE, LEAF, END, ROOT
	}

	protected NodeType type;
	protected Comparator<K> comparator;
	protected GuideNode<K, V> parent;
	protected Node<K, V> left, right;

	public Node(Comparator<K> comparator, NodeType type) {
		this.type = type;
		this.comparator = comparator;
	}

	public Comparator<K> getComparator() {
		return comparator;
	}

	public NodeType getType() {
		return type;
	}

	protected GuideNode<K, V> getParent() {
		return parent;
	}

	protected void setParent(GuideNode<K, V> parent) {
		this.parent = parent;
	}

	protected Node<K, V> getLeft() {
		return left;
	}

	protected void setLeft(Node<K, V> left) {
		this.left = left;
	}

	protected Node<K, V> getRight() {
		return right;
	}

	protected void setRight(Node<K, V> right) {
		this.right = right;
	}

	public boolean contains(K key) {
		return get(key) != null;
	}

	protected final int cmp(K arg0, K arg1) {
		return comparator.compare(arg0, arg1);
	}

	public int size() {
		throw new UnsupportedOperationException();
	}

	public boolean isFull() {
		throw new UnsupportedOperationException();
	}

	protected boolean isOverFull() {
		throw new UnsupportedOperationException();
	}

	public abstract void put(K key, V value);

	public abstract V get(K key);

	public abstract V remove(K key);

	public abstract void addToXmlDoc(Document doc, Element parent);
}