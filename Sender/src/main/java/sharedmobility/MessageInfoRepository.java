package sharedmobility;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel="messageInfos", path="messageInfos")
public interface MessageInfoRepository extends PagingAndSortingRepository<MessageInfo, Long>{


}
