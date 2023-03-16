package boot.spring.mapper;

import java.util.List;

import boot.spring.po.Item;

public interface ItemMapper {
    Item selectByPrimaryKey(Long id);

    List<Item> listItems();
}
