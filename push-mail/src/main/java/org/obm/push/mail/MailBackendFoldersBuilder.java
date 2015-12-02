package org.obm.push.mail;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.obm.push.bean.FolderType;
import org.obm.push.bean.change.hierarchy.BackendFolder;
import org.obm.push.bean.change.hierarchy.BackendFolder.BackendId;
import org.obm.push.bean.change.hierarchy.BackendFolders;
import org.obm.push.bean.change.hierarchy.MailboxPath;
import org.obm.push.mail.bean.MailboxFolder;
import org.obm.push.mail.bean.MailboxFolders;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeTraverser;


public class MailBackendFoldersBuilder {
	
	private Set<MailboxPath> mailboxes;
	private List<Entry<MailboxPath, FolderType>> specialMailboxes;

	
	public MailBackendFoldersBuilder() {
		mailboxes = Sets.newTreeSet();
		specialMailboxes = Lists.newArrayList();
	}
	
	public MailBackendFoldersBuilder addFolders(MailboxFolders folders) {
		for (MailboxFolder folder : folders) {
			mailboxes.add(MailboxPath.of(folder.getName(), folder.getImapSeparator()));
		}
		return this;
	}
	
	public MailBackendFoldersBuilder addSpecialFolder(String path, FolderType type) {
		specialMailboxes.add(Maps.immutableEntry(MailboxPath.of(path), type));
		return this;
	}
	
	public MailBackendFoldersBuilder addSpecialFolders(List<Entry<MailboxPath, FolderType>> folders) {
		specialMailboxes.addAll(folders);
		return this;
	}
	
	public BackendFolders build() {
		Map<MailboxPath, Node> pathToNode = Maps.newHashMap(); 
		Node root = new Node();
		
		// Handle special mailboxes first, as they can be parents of other folders
		for (Entry<MailboxPath, FolderType> folder : specialMailboxes) {
			appendNode(pathToNode, root, folder.getKey(), folder.getValue());
		}
		
		for (MailboxPath folder : Sets.difference(mailboxes, pathToNode.keySet())) {
			appendNode(pathToNode, root, folder, FolderType.USER_CREATED_EMAIL_FOLDER);
		}
		
		return new TreeBackendFolders(root);
	}

	private void appendNode(Map<MailboxPath, Node> pathToNode, Node root, MailboxPath path, FolderType type) {
		Entry<Node, Optional<BackendId>> searchParent = searchParent(pathToNode, root, path);
		Node node = new Node(BackendFolder.builder()
			.displayName(path.stripParentPath(searchParent.getValue()))
			.folderType(type)
			.backendId(path)
			.parentId(searchParent.getValue())
			.build());

		searchParent.getKey().children.add(node);
		pathToNode.put(path, node);
	}

	private Entry<Node, Optional<BackendId>> searchParent(Map<MailboxPath, Node> pathToNode,
			Node root, MailboxPath folder) {
		
		Optional<MailboxPath> find = Iterables.tryFind(folder.reducingPaths(), Predicates.in(pathToNode.keySet()));
		if (!find.isPresent()) {
			return Maps.immutableEntry(root, Optional.<BackendId>absent());
		}
		return Maps.immutableEntry(pathToNode.get(find.get()), Optional.<BackendId>of(find.get()));
	}
	
	private static class Node {
		
		final Set<Node> children;
		final Optional<BackendFolder> value;

		private Node() {
			this(null);
		}
		
		private Node(BackendFolder value) {
			this.value = Optional.fromNullable(value);
			this.children = Sets.newHashSet();
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(value, children);
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Node) {
				Node that = (Node)obj;
				return Objects.equal(value, that.value)
					&& Objects.equal(children, that.children);
			}
			return false;
		}
	
		@Override
		public String toString() {
			return Objects.toStringHelper(this)
				.add("value", value)
				.add("children", children)
				.toString();
		}
	}

	private static class TreeBackendFolders implements BackendFolders {
		
		private final Node root;
		
		private TreeBackendFolders(Node root) {
			this.root = root;
		}
	
		@Override
		public Iterator<BackendFolder> iterator() {
			return new TreeTraverser<Node>() {
	
				@Override
				public Iterable<Node> children(Node node) {
					return node.children;
				}
			}
			.breadthFirstTraversal(root)
			.filter(new Predicate<Node>() {
	
				@Override
				public boolean apply(Node input) {
					return input.value.isPresent();
				}
			})
			.transform(new Function<Node, BackendFolder>() {
	
				@Override
				public BackendFolder apply(Node input) {
					return input.value.get();
				}
			}).iterator();
		}
	
		@Override
		public String toString() {
			return Objects.toStringHelper(this)
				.add("root", root)
				.toString();
		}
	}	
}