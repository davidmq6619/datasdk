package boot.spring.service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import boot.spring.po.Actor;

public interface ActorService {
	List<Actor> getpageActors(int pagenum, int pagesize);

	List<Map<String, Object>> listActorMap();

	int getactornum();

	Actor getActorByid(short id);

	Actor updateactor(Actor a);

	Actor addactor(Actor a);

	void delete(short id);

	// 耗时的操作使用异步任务
	String asyncTask();

	public String task();
}
