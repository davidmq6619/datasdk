package boot.spring.service;

import java.util.List;

import boot.spring.po.Item;

public interface ItemService {
    Item getItemById(Long id);

    List<Item> listItems();

    List<Item> listItems(int pagenum, int pagesize);
}
