package org.obm.push.mail;

import org.minig.imap.StoreClient;
import org.obm.push.backend.BackendSession;
import org.obm.push.bean.FilterType;
import org.obm.push.bean.SyncState;
import org.obm.push.exception.ServerErrorException;

public interface IEmailSync {

	MailChanges getSync(StoreClient imapStore, Integer devId,
			BackendSession bs, SyncState state, Integer collectionId, FilterType filter)
			throws ServerErrorException;
}
